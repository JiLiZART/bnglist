package worker;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class BnetConnection extends Thread {
	public static int STATE_TERMINATED = -1;
	public static int STATE_DISCONNECTED = 0;
	public static int STATE_CONNECTED = 1;
	public static int STATE_LOGGEDIN = 2;

	BnetRealm realm;
	int id;
	String name;
	
	Socket socket;
	LittleEndianDataInputStream in;
	LittleEndianDataOutputStream out;
	BnetProtocol protocol;
	BNCSUtilInterface bncs;
	ByteBuffer buf;
	
	QueueThread queue;
	
	String username;
	String password;
	
	int state;
	
	public BnetConnection(BnetRealm realm, int id, String name) {
		this.realm = realm;
		this.id = id;
		this.name = name;
		state = STATE_DISCONNECTED;
		buf = ByteBuffer.allocate(65536);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		
		username = Config.getString(getKey("username"), null);
		password = Config.getString(getKey("password"), null);
		
		if(username != null && password != null) {
			bncs = new BNCSUtilInterface(username, password);
		} else {
			println("Error: username or password for this connection is unset");
			state = STATE_TERMINATED;
		}
	}
	
	public void println(String message) {
		Main.println("[" + realm.getName() + " " + name + "] " + message);
	}
	
	//returns the prefix in CFG
	public String getKey(String key) {
		if(id == -1) return "bnet_" + key;
		else return "bnet" + id + "_" + key;
	}

	public boolean connect(String hostname, int port) {
		println("Connecting to " + hostname + " on port " + port);

		try {
			socket = new Socket(hostname, port);
			in = new LittleEndianDataInputStream(socket.getInputStream());
			out = new LittleEndianDataOutputStream(socket.getOutputStream());
		} catch(IOException ioe) {
			println("Error while connecting to " + hostname + ": " + ioe.getLocalizedMessage());
			return false;
		}

		println("Initializing protocol and auth info");

		protocol = new BnetProtocol(in, out);
		int war3version = Config.getInt(getKey("war3version"), 26);
		boolean tft = Config.getBoolean("bot_tft", true);
		int localeID = Config.getInt(getKey("locale"), 1033);
		String countryAbbrev = Config.getString(getKey("countryabbrev"), "USA");
		String country = Config.getString(getKey("country"), "United States");
		
		try {
			protocol.sendProtocolInitializeSelector();
			protocol.sendAuthInfo((byte) war3version, tft, localeID, countryAbbrev, country);
		} catch(IOException ioe) {
			println("Error while initializing protocol and auth info: " + ioe.getLocalizedMessage());
			return false;
		}

		println("Connected");
		
		state = STATE_CONNECTED;
		return true;
	}
	
	public void terminate(String reason) {
		if(reason == null) reason = "unknown";
		
		println("Terminating connection: " + reason);
		state = STATE_TERMINATED;

		try {
			socket.close();
		} catch(IOException e) {}
	}
	
	public void run() {
		while(state != STATE_TERMINATED) {
			try {
				int headerConstant = in.readUnsignedByte();
				if(headerConstant != BnetProtocol.BNET_HEADER_CONSTANT) terminate("received invalid header constant");
			
				int packetType = in.readUnsignedByte();
				int packetLength = in.readUnsignedShort();
				int dataLength = packetLength - 4;
					
				buf.clear();
				in.readFully(buf.array(), 0, dataLength);
				buf.position(0);
			
				if(packetType == BnetProtocol.SID_AUTH_INFO) {
					if(protocol.receiveAuthInfo(buf, dataLength)) {
						String rocKey = Config.getString(getKey("cdkeyroc"), null);
						String tftKey = Config.getString(getKey("cdkeytft"), null);
						
						if(bncs.HELP_SID_AUTH_CHECK(Config.tft, Config.war3path, rocKey, tftKey, Util.toStr(protocol.getValueStringFormula()), Util.toStr(protocol.getIX86VerFileName()), Util.toInt(protocol.getClientToken()), Util.toInt(protocol.getServerToken()))) {
							if(Config.tft)
								println("Attempting to auth as Warcraft III: The Frozen Throne");
							else
								println("Attempting to auth as Warcraft III: Reign of Chaos");
							
							protocol.sendAuthCheck(Config.tft, bncs.GetEXEVersion(), bncs.GetEXEVersionHash(), bncs.GetKeyInfoROC(), bncs.GetKeyInfoTFT(), bncs.GetEXEInfo(), "GHost");
						}
					}
				} else if(packetType == BnetProtocol.SID_AUTH_CHECK) {
					if(protocol.receiveAuthCheck(buf, dataLength)) {
						println("CD keys accepted");
						bncs.HELP_SID_AUTH_ACCOUNTLOGON();
						protocol.sendAuthAccountLogon(bncs.GetClientKey(), username);
					} else {
						//cd keys not accepted
						
						switch(Util.toInt(protocol.getKeyState()))
						{
						case BnetProtocol.ROC_KEY_IN_USE:
							println("logon failed - ROC CD key in use by user [" + protocol.getKeyStateDescription() + "], disconnecting");
							break;
						case BnetProtocol.TFT_KEY_IN_USE:
							println("logon failed - TFT CD key in use by user [" + protocol.getKeyStateDescription() + "], disconnecting");
							break;
						case BnetProtocol.OLD_GAME_VERSION:
							println("logon failed - game version is too old, disconnecting");
							break;
						case BnetProtocol.INVALID_VERSION:
							println("logon failed - game version is invalid, disconnecting");
							break;
						default:
							println("logon failed - cd keys not accepted, disconnecting");
							break;
						}
						
						terminate("logon failed");
					}
				} else if(packetType == BnetProtocol.SID_AUTH_ACCOUNTLOGON) {
					if(protocol.receiveAuthAccountLogon(buf, dataLength)) {
						println("Username [" + username + "] accepted");
						println("Using battle.net logon type (for official battle.net servers only)");
						
						bncs.HELP_SID_AUTH_ACCOUNTLOGONPROOF(Util.toStr(protocol.getSalt()), Util.toStr(protocol.getServerPublicKey()));
						protocol.sendAuthAccountLogonProof(bncs.GetM1());
					} else {
						println("logon failed - invalid username, disconnecting");
						terminate("logon failed");
					}
				} else if(packetType == BnetProtocol.SID_AUTH_ACCOUNTLOGONPROOF) {
					if(protocol.receiveAuthAccountLogonProof(buf, dataLength)) {
						println("Logon successful");
						state = STATE_LOGGEDIN;

						protocol.sendNetGamePort(Config.hostport);
						protocol.sendEnterChat();
						
						println("Starting queue thread");
						queue = new QueueThread(this);
						queue.start();
					} else {
						println("logon failed - invalid password, disconnecting");
						terminate("logon failed");
					}
				} else if(packetType == BnetProtocol.SID_GETADVLISTEX) {
					ArrayList<IncomingGameHost> games = protocol.receiveGetAdvListex3(buf, dataLength);
					
					if(games != null) {
						println("Submitting " + games.size() + " games");
						
						for(IncomingGameHost game : games) {
							realm.client.sendGame(game);
						}
					}
				}
			} catch(IOException ioe) {
				println("Socket error: " + ioe.getLocalizedMessage());
				terminate("socket error");
			}
		}
	}
}

class QueueThread extends Thread {
	BnetConnection connection;
	Queue<QueueCommand> commands;
	
	public QueueThread(BnetConnection connection) {
		this.connection = connection;
		commands = new LinkedList<QueueCommand>();
	}
	
	public void queue(QueueCommand command) {
		if(commands.size() > 10) {
			Main.println("[Queue] Queue size is " + commands.size() + ", ignoring type=" + command.type);
		} else {
			synchronized(commands) {
				commands.add(command);
				commands.notify();
			}
		}
	}
	
	public void execute(QueueCommand command) throws IOException {
		if(command.type == QueueCommand.COMMAND_CHAT) {
			
		} else if(command.type == QueueCommand.COMMAND_LIST) {
			CommandList listCommand = (CommandList) command;
			connection.protocol.sendGetAdvListex3(listCommand.gamename, listCommand.numGames);
		}
	}
	
	public void run() {
		while(true) {
			QueueCommand command;
			
			if(!commands.isEmpty()) {
				synchronized(commands) {
					command = commands.poll();
				}
			} else {
				Main.println("[Queue] Empty queue, getting game list instead");
				command = new CommandList("", 20);
			}
				
			try {
				execute(command);
			} catch(IOException e) {
				connection.terminate("write error: " + e.getLocalizedMessage());
			}
			
			try {
				Thread.sleep(command.getWaitTicks());
			} catch(InterruptedException e) {
				//this shouldn't occur
				Main.println("[Queue] Interrupted while sleeping on type=" + command.type + ", wait=" + command.getWaitTicks());
			}
		}
	}
}

class QueueCommand {
	public static final int COMMAND_CHAT = 0;
	public static final int COMMAND_LIST = 2;
	
	int type;
	
	public int getWaitTicks() {
		return 5000;
	}
}

class CommandChat extends QueueCommand {
	String message;
	
	public CommandChat(String message) {
		type = COMMAND_CHAT;
		this.message = message;
	}
	
	public int getWaitTicks() {
		return 1000 + message.length() * 24;
	}
}

class CommandWhisper extends CommandChat {
	public CommandWhisper(String user, String message) {
		super("/w " + user + " " + message);
	}
}

class CommandList extends QueueCommand {
	String gamename;
	int numGames;
	
	public CommandList(String gamename, int numGames) {
		type = COMMAND_LIST;
		this.gamename = gamename;
		this.numGames = numGames;
	}
	
	public int getWaitTicks() {
		return 20000;
	}
}
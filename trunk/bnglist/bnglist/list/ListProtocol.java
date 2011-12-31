package bnglist.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import bnglist.Config;
import bnglist.Game;
import bnglist.Games;
import bnglist.Main;
import bnglist.util.Util;


public class ListProtocol {
	public static final int STATE_INITIATED = 0;
	public static final int STATE_HELLO = 1;
	public static final int STATE_AUTHENTICATED = 2;
	
	public static final int PULL_STATUS_ADD = 0;
	public static final int PULL_STATUS_REPLACE = 1;
	public static final int PULL_STATUS_DELETE = 2;
	
	//note that USERPASS is only useful for identifying users
	// with only one user, the same thing can be accomplished with PASSKEY
	public static final int AUTHENTICATE_NONE = 0;
	public static final int AUTHENTICATE_PASSKEY = 1;
	public static final int AUTHENTICATE_USERPASS = 2;
	public static final int AUTHENTICATE_USERHASH = 3;
	
	public static final int JOIN_DISABLED = 0;
	public static final int JOIN_STANDARD = 1; //join method where join and leave messages will be sent to battle.net
	public static final int JOIN_WHISPER = 2; //join method where "sc" will be sent to hostname
	
	BufferedReader in;
	PrintStream out;
	String identifier; //IP address to identify this connection
	int state; //see STATE_ constants
	boolean pullActive; //true if pull is activated
	
	ListConnection connection;
	
	//config
	ArrayList<String> commands;
	int joinMethod;
	
	int authenticateMethod;
	String authenticatePasskey; //null if authenticateMethod != AUTHENTICATE_PASSKEY
	HashMap<String, String> authenticateUsers; //map from user to pass if authenticateMethod = USERPASS or USERHASH
	String authenticateSalt;
	
	public ListProtocol(BufferedReader in, PrintStream out, String identifier, ListConnection connection) {
		this.in = in;
		this.out = out;
		this.identifier = identifier;
		this.connection = connection;
		state = STATE_INITIATED;
		pullActive = false;
		
		//config
		commands = new ArrayList<String>();
		commands.add("HELLO");
		commands.add("AUTHENTICATE");
		commands.add("COMMANDS");
		commands.add("GAMELIST");
		commands.add("DETAILS");
		commands.add("PULL");
		
		//authentication method
		authenticateMethod = Config.getInt("server_auth", AUTHENTICATE_NONE);
		
		if(authenticateMethod == AUTHENTICATE_PASSKEY) {
			authenticatePasskey = Config.getString("server_auth_passkey", null);
			
			if(authenticatePasskey == null) {
				Main.println("[ListProtocol] Warning: authentication passkey is null, authentication will be impossible");
			}
		} else if(authenticateMethod == AUTHENTICATE_USERPASS || authenticateMethod == AUTHENTICATE_USERHASH) {
			String[] users = Config.getString("server_auth_users", "").split(" ");
			String[] passwords = Config.getString("server_auth_passwords", "").split(" ");
			authenticateUsers = new HashMap<String, String>();
			
			for(int i = 0; i < Math.min(users.length, passwords.length); i++) {
				if(authenticateMethod == AUTHENTICATE_USERHASH) {
					passwords[i] = Util.getSha1Result(passwords[i]);
				}
				
				authenticateUsers.put(users[i].toLowerCase(), passwords[i]);
			}
			
			if(authenticateUsers.size() == 0) {
				Main.println("[ListProtocol] Warning: authentication userlist is empty, authentication will be impossible");
			}
			
			if(authenticateMethod == AUTHENTICATE_USERHASH) {
				authenticateSalt = Util.getHex(Util.randomBytes(32));
			}
		}
		
		//join method
		joinMethod = Config.getInt("server_join", 0);
		
		if(joinMethod != JOIN_DISABLED) {
			commands.add("JOIN");
		}
	}
	
	public void sendHello() {
		out.println("HELLO " + Main.BNGLIST_VERSION);
		//todo: add optional message
	}
	
	public void sendGamelist() {
		out.println(connection.server.games.getGamelistString());
		//todo: anti-flood protection
	}
	
	public void sendDetails(int id) {
		out.println(connection.server.games.getGameDetails(id));
	}
	
	public void sendAuthenticateMethod() {
		String doPrint = authenticateMethod + "";
		
		if(authenticateMethod == AUTHENTICATE_USERHASH) {
			doPrint += " " + authenticateSalt;
		}
		
		out.println(doPrint);
	}
	
	public void sendCommands() {
		out.print(commands.get(0)); //commands.size() >= 6
		for(int i = 1; i < commands.size(); i++) {
			out.print(" " + commands.get(i));
		}
		
		out.println();
	}
	
	public void sendPullTrigger(int status, int id, Game g) {
		if(status == PULL_STATUS_ADD) {
			out.println("PULL ADD " + id + "|" + g.getCodeString());
		} else if(status == PULL_STATUS_REPLACE) {
			out.println("PULL REPLACE " + id + "|" + g.getCodeString());
		} else if(status == PULL_STATUS_DELETE) {
			out.println("DELETE " + id);
		} else {
			Main.println("[ListProtocol " + identifier + "] Invalid status " + status + " in sendPullTrigger()");
		}
	}
	
	public void sendSay(String username) {
		if(username != null) out.println(username);
		else out.println();
	}
	
	public boolean handleCommand() throws IOException {
		String line = in.readLine();
		
		if(line == null) {
			throw new IOException("end of stream reached");
		}
		
		String[] parts = line.split(" ", 2);
		String command = parts[0];
		
		String payload = "";
		if(parts.length > 1)
			payload = parts[1];
		
		if(state == STATE_INITIATED) {
			if(command.equalsIgnoreCase("HELLO")) {
				Main.println("[ListProtocol " + identifier + "] Accepted HELLO; client version is " + payload);
				
				sendHello();
				state = STATE_HELLO;
				
				return true;
			}
		} else if(state == STATE_HELLO) {
			if(command.equalsIgnoreCase("AUTHENTICATE")) {
				if(payload.isEmpty()) {
					sendAuthenticateMethod();
					if(authenticateMethod == AUTHENTICATE_NONE) {
						Main.println("[ListProtocol " + identifier + "] Authenticated through no authentication");
						state = STATE_AUTHENTICATED;
					}
					
					return true;
				} else if(authenticateMethod == AUTHENTICATE_PASSKEY && authenticatePasskey != null) {
					if(payload.equals(authenticatePasskey)) {
						Main.println("[ListProtocol " + identifier + "] Authenticated through valid passkey");
						state = STATE_AUTHENTICATED;
						return true;
					}
				} else if(authenticateMethod == AUTHENTICATE_USERPASS || authenticateMethod == AUTHENTICATE_USERHASH) {
					String[] payloadParts = payload.split(" ", 2);
					String username = payloadParts[0];
					String password = payloadParts[1];
					
					String actualPassword = authenticateUsers.get(username.toLowerCase());
					
					if(actualPassword != null) {
						if(authenticateMethod == AUTHENTICATE_USERPASS) {
							if(password.equals(actualPassword)) {
								Main.println("[ListProtocol " + identifier + "] Authenticated through valid userpass: " + username);
								return true;
							}
						} else if(authenticateMethod == AUTHENTICATE_USERHASH && authenticateSalt != null) {
							String passwordHash = Util.getSha1Result(password);
							
							if(passwordHash.equals(actualPassword)) {
								Main.println("[ListProtocol " + identifier + "] Authenticated through valid userhash: " + username);
								return true;
							}
						}
					}
				}
			}
		} else if(state == STATE_AUTHENTICATED) {
			if(command.equalsIgnoreCase("NOOP")) {
				return true;
			} else if(command.equalsIgnoreCase("GAMELIST")) {
				sendGamelist();
				return true;
			} else if(command.equalsIgnoreCase("DETAILS")) {
				try {
					int id = Integer.parseInt(payload);
					sendDetails(id);
					return true;
				} catch(NumberFormatException nfe) {}
			} else if(command.equalsIgnoreCase("COMMANDS")) {
				sendCommands();
				return true;
			} else if(command.equalsIgnoreCase("PULL")) {
				if(!pullActive) connection.server.games.registerListener(connection);
				else connection.server.games.deregisterListener(connection);
				
				return true;
			} else if(joinMethod != JOIN_DISABLED && command.equals("JOIN")) {
				try {
					int id = Integer.parseInt(payload);
					Game g = connection.server.games.getGame(id);
					
					if(connection.server.workerServer != null && g != null) {
						//tryJoin returns the username that should be used
						// it may return null or blank string on fail; both of these are handled
						sendSay(connection.server.workerServer.tryJoin(g, joinMethod));
					} else {
						sendSay(null);
					}
					
					return true;
				} catch(NumberFormatException nfe) {}
			}
		}
		
		return false;
	}
}

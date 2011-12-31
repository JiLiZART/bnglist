package worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

import bnglist.util.Util;

public class WorkerClient extends Thread {
	public static final int STATE_TERMINATED = -1;
	public static final int STATE_INITIALIZED = 0;
	public static final int STATE_CONNECTED = 1;
	
	String hostname;
	int port;
	int state;
	
	Socket socket;
	BufferedReader in;
	PrintStream out;
	
	ArrayList<BnetRealm> realms;
	
	public WorkerClient(ArrayList<BnetRealm> realms) {
		this.realms = realms;
		
		hostname = Config.getString("worker_hostname", null);
		port = Config.getInt("worker_hostport", 5873);
		state = STATE_INITIALIZED;
	}
	
	public void init() {
		Main.println("[Client] Connecting to " + hostname + " on port " + port);
		
		try {
			if(hostname == null) {
				Main.println("[Client] Host name not set, connecting on loopback");
				socket = new Socket(Util.getLoopbackAddress(), port);
			} else {
				socket = new Socket(hostname, port);
			}
			
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintStream(socket.getOutputStream(), true);
			state = STATE_CONNECTED;
			
			sendHello();
		} catch(IOException ioe) {
			Main.println("[Client] Connect error: " + ioe.getLocalizedMessage());
		}
	}
	
	public void sendHello() {
		if(state == STATE_CONNECTED) {
			out.println("HELLO " + Main.BNGLIST_VERSION);
		}
	}
	
	public void sendGame(IncomingGameHost game, BnetRealm realm) {
		if(state == STATE_CONNECTED) {
			out.println("PUSH " + game.getCodeString(realm));
		}
	}
	
	public void sendSay(String user) {
		if(user == null) out.println("SAY");
		else out.println("SAY " + user);
	}
	
	public void terminate() {
		if(state == STATE_TERMINATED) return;
		
		state = STATE_TERMINATED;
		
		try {
			socket.close();
		} catch(IOException ioe) {}
	}
	
	public void run() {
		while(state == STATE_CONNECTED) {
			try {
				String str = in.readLine();
				
				if(str != null) {
					String[] parts = str.split(" ", 2);
					
					String command = parts[0];
					String payload = "";
					if(parts.length == 2) payload = parts[1];
					
					if(command.equalsIgnoreCase("HELLO")) {
						Main.println("[Client] Received server HELLO; version: " + payload);
					} else if(command.equalsIgnoreCase("SAY")) {
						String[] payloadParts = payload.split(" ", 2);
						
						if(payloadParts.length == 2) {
							int realm = Util.parseInt(payloadParts[0]);
							String chatCommand = payloadParts[1];
							
							String user = null;
							for(int i = 0; i < realms.size(); i++) {
								if(realms.get(i).id == realm) {
									user = realms.get(i).fastChatCommand(chatCommand);
									break;
								}
							}
							
							sendSay(user);
						}
					}
				} else {
					Main.println("[Client] Read error");
					terminate();
				}
			} catch(IOException ioe) {
				Main.println("[Client] Read error: " + ioe.getLocalizedMessage());
				terminate();
				//todo: reconnect
			}
		}
	}
}

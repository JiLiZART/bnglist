package worker;

import java.util.ArrayList;


public class BnetRealm {
	ArrayList<BnetConnection> connections;
	int id;
	String name;
	WorkerClient client;

	public BnetRealm(int id, String name, WorkerClient client) {
		this.id = id;
		this.name = name;
		this.client = client;

		connections = new ArrayList<BnetConnection>();
	}
	
	public void println(String message) {
		Main.println("[Realm " + name + "] " + message);
	}
	
	public String getKey(String key) {
		if(id == -1) return "realm_" + key;
		else return "realm" + id + "_" + key;
	}
	
	public void createConnection(int id) {
		println("Creating connection " + id);
		String connectionName = Config.getString(getKey("bnet" + id), id + "");
		BnetConnection connection = new BnetConnection(this, id, connectionName);
		
		if(connection.state != BnetConnection.STATE_TERMINATED) {
			connections.add(connection);

			String hostname = Config.getString(connection.getKey("hostname"), "useast.battle.net");
			int port = Config.getInt(connection.getKey("port"), 6112);

			connection.connect(hostname, port);
			connection.start();
		}
	}
	
	//this is used for JOIN messages; differences from chatCommand:
	// first, it will only accept connections with less than two elements in queue
	// second, it will delay 1 second before sending message
	public String fastChatCommand(String message) {
		//find connection with < 2 in queue, or first connection with empty queue
		int minimumLength = 2;
		BnetConnection minimumConnection = null;
		
		for(int i = 0; i < connections.size(); i++) {
			if(connections.get(i).queue.size() < minimumLength) {
				minimumConnection = connections.get(i);
				
				if(minimumConnection.queue.size() == 0) {
					minimumConnection.delay(1000);
					minimumConnection.chatCommand(message);
					return minimumConnection.username;
				}
				
				minimumLength = minimumConnection.queue.size();
			}
		}
		
		if(minimumConnection != null) {
			minimumConnection.delay(1000);
			minimumConnection.chatCommand(message);
			return minimumConnection.username;
		} else {
			return null; //not fast enough
		}
	}
	
	public void chatCommand(String message) {
		//find connection with minimum queue, or first connection with empty queue
		int minimumLength = Integer.MAX_VALUE;
		BnetConnection minimumConnection = null;
		
		for(int i = 0; i < connections.size(); i++) {
			if(connections.get(i).queue.size() < minimumLength) {
				minimumConnection = connections.get(i);
				
				if(minimumConnection.queue.size() == 0) {
					minimumConnection.chatCommand(message);
					return;
				}
				
				minimumLength = minimumConnection.queue.size();
			}
		}
		
		if(minimumConnection != null) {
			minimumConnection.chatCommand(message);
		} else {
			println("Error: it seems as though there are no active connections (n=" + connections.size() + ")");
		}
	}
	
	public void chatCommand(String message, String user, boolean whisper) {
		if(whisper) {
			if(!user.contains("\\/") && !user.contains("\\|") && !user.contains(" ")) {
				chatCommand("/w " + user + " " + message);
			}
		} else {
			chatCommand(message);
		}
	}
	
	//public void createGame(byte state, String gamename, String hostname, Map map, int hostcounter) {}
	public void destroyGame(int hostcounter) {}
	
	public String getName() {
		return name;
	}
}

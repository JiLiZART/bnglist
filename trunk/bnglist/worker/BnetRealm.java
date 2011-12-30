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
	
	public void chatCommand(String message) {}
	public void chatCommand(String message, String user, boolean whisper) {}
	
	//public void createGame(byte state, String gamename, String hostname, Map map, int hostcounter) {}
	public void destroyGame(int hostcounter) {}
	
	public String getName() {
		return name;
	}
}

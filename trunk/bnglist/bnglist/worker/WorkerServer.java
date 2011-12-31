package bnglist.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import bnglist.Config;
import bnglist.Game;
import bnglist.Games;
import bnglist.Main;
import bnglist.list.ListProtocol;
import bnglist.util.Util;


public class WorkerServer extends Thread {
	public static final int DEFAULT_PORT = 5873;
	
	ServerSocket server;
	Games games;
	ArrayList<WorkerConnection> connections;
	
	//config
	int port;
	ArrayList<InetAddress> whitelist;
	
	public WorkerServer(Games games) {
		this.games = games;
		connections = new ArrayList<WorkerConnection>();
		
		port = Config.getInt("worker_port", DEFAULT_PORT);
		String whitelistString = Config.getString("worker_whitelist", "localhost");
		
		if(whitelistString == null || whitelistString.isEmpty()) {
			whitelist = null;
		} else if(whitelistString.equals("localhost")) {
			whitelist = new ArrayList<InetAddress>();
		} else {
			String[] whitelistParts = whitelistString.split(" ");
			
			for(String element : whitelistParts) {
				try {
					InetAddress address = InetAddress.getByName(element);
					whitelist.add(address);
				} catch(UnknownHostException uhe) {
					Main.println("[WorkerServer] Warning: unable to getbyname on " + element);
				}
			}
			
			Main.println("[WorkerServer] Loaded " + whitelist.size() + " whitelist addresses");
		}
	}
	
	public void init() {
		Main.println("[WorkerServer] Initiating on port " + port);
		
		try {
			if(whitelist != null && whitelist.size() == 0) { //only listen to localhost
				Main.println("[WorkerServer] Listening on loopback only");
				server = new ServerSocket(port, 50, Util.getLoopbackAddress());
			} else {
				server = new ServerSocket(port);
			}
		} catch(IOException ioe) {
			Main.println("[WorkerServer] Error while binding: " + ioe.getLocalizedMessage());
		}
	}
	
	public String tryJoin(Game game, int method) {
		if(method == ListProtocol.JOIN_STANDARD) {
			Main.println("[WorkerServer] JOIN_STANDARD is currently unsupported");
			return null;
		} else if(method == ListProtocol.JOIN_WHISPER) {
			if(!game.hostName.contains("/") && !game.hostName.contains(" ")) {
				String response = null;
				
				synchronized(connections) { //we don't want to find a random one and then find out it was deleted
					int rand = (int) (Math.random() * connections.size());
					response = connections.get(rand).sendSay(game.realm, "/w " + game.hostName + " s"); //spoof check message
				}
				
				return response;
			} else {
				Main.println("[WorkerServer] Detected bad username [" + game.hostName + "]; aborting JOIN");
				return null;
			}
		} else {
			Main.println("[WorkerServer] Unknown join method, " + method);
			return null;
		}
	}
	
	public void removeConnection(WorkerConnection connection) {
		synchronized(connections) {
			connections.remove(connection);
		}
	}
	
	public void run() {
		while(true) {
			try {
				Socket socket = server.accept();
				
				if(whitelist != null && whitelist.size() > 0 && whitelist.contains(socket.getInetAddress())) {
					Main.println("[WorkerServer] Rejecting connection from " + socket.getInetAddress() + " due to whitelist");
					socket.close();
					continue;
				}
				
				Main.println("[WorkerServer] New connection from " + socket.getInetAddress());
				WorkerConnection connection = new WorkerConnection(socket, this);
				
				synchronized(connections) {
					connections.add(connection);
				}
				
				connection.start();
			} catch(IOException ioe) {
				Main.println("[WorkerServer] Error while accepting connection: " + ioe.getLocalizedMessage());
			}
		}
	}
}

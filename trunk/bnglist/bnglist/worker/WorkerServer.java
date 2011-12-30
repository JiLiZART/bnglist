package bnglist.worker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import bnglist.Config;
import bnglist.Games;
import bnglist.Main;
import bnglist.list.ListConnection;
import bnglist.util.Util;


public class WorkerServer extends Thread {
	public static final int DEFAULT_PORT = 5873;
	
	ServerSocket server;
	Games games;
	
	//config
	int port;
	ArrayList<InetAddress> whitelist;
	
	public WorkerServer(Games games) {
		this.games = games;
		
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
				connection.start();
			} catch(IOException ioe) {
				Main.println("[WorkerServer] Error while accepting connection: " + ioe.getLocalizedMessage());
			}
		}
	}
}

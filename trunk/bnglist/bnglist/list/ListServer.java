package bnglist.list;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import bnglist.Config;
import bnglist.Games;
import bnglist.Main;
import bnglist.worker.WorkerServer;


public class ListServer extends Thread {
	public static final int DEFAULT_PORT = 5872;
	
	ServerSocket server;
	Integer numConnections;
	Games games;
	WorkerServer workerServer;
	
	//config
	int maxConnections; //maximum number of connections to allow, or 0 for unlimited
	int port;
	
	public ListServer(Games games) {
		this.games = games;
		numConnections = 0;
		
		maxConnections = Config.getInt("server_maxconnections", 0);
		port = Config.getInt("server_port", DEFAULT_PORT);
	}
	
	public void init() {
		Main.println("[Server] Initiating on port " + port);
		
		try {
			server = new ServerSocket(port);
		} catch(IOException ioe) {
			Main.println("[Server] Error while binding: " + ioe.getLocalizedMessage());
		}
	}
	
	public void setWorkerServer(WorkerServer workerServer) {
		this.workerServer = workerServer;
	}
	
	public void removeConnection() {
		synchronized(numConnections) {
			numConnections--;
		}
		
		Main.println("[Server] Removed connection, now at " + numConnections + " connections");
	}
	
	public void run() {
		while(true) {
			try {
				Socket socket = server.accept();
				
				if(maxConnections != 0 && numConnections >= maxConnections) {
					//todo: option to send a message before closing socket
					Main.println("[Server] Rejecting connection from " + socket.getInetAddress() + " due to limit");
					socket.close();
					continue;
				}
				
				synchronized(numConnections) {
					numConnections++;
				}
				
				Main.println("[Server] New connection from " + socket.getInetAddress() + "; now at " + numConnections + " connections");
				ListConnection connection = new ListConnection(socket, this);
				connection.start();
			} catch(IOException ioe) {
				Main.println("[Server] Error while accepting connection: " + ioe.getLocalizedMessage());
			}
		}
	}
}

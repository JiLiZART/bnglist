package bnglist.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import bnglist.Config;
import bnglist.Game;
import bnglist.GameUpdateListener;
import bnglist.Main;
import bnglist.util.BoundedBufferedReader;


public class ListConnection extends Thread implements GameUpdateListener {
	Socket socket;
	BufferedReader in;
	PrintStream out;
	
	ListProtocol protocol;
	ListServer server;
	
	//config
	int aliveTime; //after this idle time (ms), client is disconnected, or 0 for unlimited
	
	public ListConnection(Socket socket, ListServer server) throws IOException {
		this.socket = socket;
		this.server = server;
		in = new BoundedBufferedReader(new InputStreamReader(socket.getInputStream()), 4096);
		out = new PrintStream(socket.getOutputStream(), true); //true for autoflush
		
		protocol = new ListProtocol(in, out, socket.getInetAddress().toString(), this);
		
		aliveTime = Config.getInt("server_alivetime", 30000);
		//this will cause socket.read to trigger SocketTimeoutException after aliveTime idle
		// this will go to our catch(IOException) block, which is good
		socket.setSoTimeout(aliveTime);
	}
	
	public void println(String message) {
		Main.println("[Connection " + socket.getInetAddress() + "] " + message);
	}
	
	public void run() {
		while(true) {
			try {
				boolean success = protocol.handleCommand();
				if(!success) {
					println("Terminating connection due to protocol failure");
					break;
				}
			} catch(IOException ioe) {
				println("Connection terminated: " + ioe.getLocalizedMessage());
				break;
			}
		}
		
		try {
			socket.close();
		} catch(IOException ioe) {}
		
		server.removeConnection();
	}
	
	public void gameReplaced(int id, Game g) {
		protocol.sendPullTrigger(ListProtocol.PULL_STATUS_REPLACE, id, g);
	}
	
	public void gameAdded(int id, Game g) {
		protocol.sendPullTrigger(ListProtocol.PULL_STATUS_ADD, id, g);
	}
	
	public void gameDeleted(int id) {
		protocol.sendPullTrigger(ListProtocol.PULL_STATUS_DELETE, id, null);
	}
}

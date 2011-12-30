package bnglist.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import bnglist.Games;
import bnglist.Main;
import bnglist.util.BoundedBufferedReader;


public class ListConnection extends Thread {
	Socket socket;
	BufferedReader in;
	PrintStream out;
	ListProtocol protocol;
	ListServer server;
	
	//config
	int aliveTime; //after this idle time (ms), client is disconnected, or 0 for unlimited
	//todo alivetime
	
	public ListConnection(Socket socket, ListServer server) throws IOException {
		this.socket = socket;
		this.server = server;
		in = new BoundedBufferedReader(new InputStreamReader(socket.getInputStream()), 4096);
		out = new PrintStream(socket.getOutputStream(), true); //true for autoflush
		
		protocol = new ListProtocol(in, out, server.games);
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
}

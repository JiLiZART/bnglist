package bnglist.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import bnglist.Games;
import bnglist.Main;
import bnglist.util.BoundedBufferedReader;


public class WorkerConnection extends Thread {
	Socket socket;
	BufferedReader in;
	PrintStream out;
	WorkerProtocol protocol;
	WorkerServer server;
	
	public WorkerConnection(Socket socket, WorkerServer server) throws IOException {
		this.socket = socket;
		this.server = server;
		in = new BoundedBufferedReader(new InputStreamReader(socket.getInputStream()), 4096);
		out = new PrintStream(socket.getOutputStream(), true); //true for autoflush
		
		protocol = new WorkerProtocol(in, out, server.games);
	}
	
	public void println(String message) {
		Main.println("[Worker " + socket.getInetAddress() + "] " + message);
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
	}
}

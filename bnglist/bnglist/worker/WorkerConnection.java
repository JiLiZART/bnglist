package bnglist.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import bnglist.Main;
import bnglist.util.BoundedBufferedReader;


public class WorkerConnection extends Thread {
	Socket socket;
	BufferedReader in;
	PrintStream out;
	WorkerProtocol protocol;
	WorkerServer server;
	
	boolean terminated;
	
	Integer sayWait;
	String sayResponse;
	
	public WorkerConnection(Socket socket, WorkerServer server) throws IOException {
		this.socket = socket;
		this.server = server;
		in = new BoundedBufferedReader(new InputStreamReader(socket.getInputStream()), 4096);
		out = new PrintStream(socket.getOutputStream(), true); //true for autoflush
		
		sayWait = 0;
		protocol = new WorkerProtocol(in, out, this);
	}
	
	public void println(String message) {
		Main.println("[Worker " + socket.getInetAddress() + "] " + message);
	}
	
	public String sendSay(int realm, String message) {
		if(terminated) {
			return "";
		}
		
		synchronized(sayWait) {
			if(sayResponse != null) { //this should never occur since this is synchronized with sayWait
				sayResponse = null;
				println("Warning: sayResponse is not null in sendSay");
			}
		
			protocol.sendSay(realm, message);
			
			//now, we wait for the message to be received
			// we might have terminated during this loop, but on terminate we notify sayWait
			synchronized(sayWait) {
				while(sayResponse == null && !terminated) {
					try {
						sayWait.wait();
					} catch(InterruptedException e) {}
				}
			}
		}
		
		String response = sayResponse;
		sayResponse = null;
		return response;
	}
	
	public void terminate() {
		if(terminated) return; //already terminated or terminating
		
		terminated = true;
		
		//make sure that any sendSay waiting call stops
		synchronized(sayWait) {
			sayWait.notify();
		}
		
		try {
			socket.close();
		} catch(IOException ioe) {}
	}
	
	public void run() {
		while(!terminated) {
			try {
				boolean success = protocol.handleCommand();
				if(!success) {
					println("Terminating connection due to protocol failure");
					terminate();
				}
			} catch(IOException ioe) {
				println("Connection terminated: " + ioe.getLocalizedMessage());
				terminate();
			}
		}
	}
}

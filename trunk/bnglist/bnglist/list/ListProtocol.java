package bnglist.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import bnglist.Games;
import bnglist.Main;


public class ListProtocol {
	public static final int STATE_INITIATED = 0;
	public static final int STATE_HELLO = 1;
	//todo: add simple plaintext authentication
	
	BufferedReader in;
	PrintStream out;
	Games games;
	int state;
	
	public ListProtocol(BufferedReader in, PrintStream out, Games games) {
		this.in = in;
		this.out = out;
		this.games = games;
	}
	
	public void sendHello() {
		out.println("HELLO " + Main.BNGLIST_VERSION);
		//todo: add optional message
	}
	
	public void sendGamelist() {
		out.println(games.getGamelistString());
		//todo: anti-flood protection
	}
	
	public void sendDetails(int id) {
		out.println(games.getGameDetails(id));
	}
	
	public boolean handleCommand() throws IOException {
		String line = in.readLine();
		
		if(line == null) {
			throw new IOException("end of stream reached");
		}
		
		String[] parts = line.split(" ", 2);
		String command = parts[0];
		
		String payload = "";
		if(parts.length > 1)
			payload = parts[1];
		
		if(state == STATE_INITIATED) {
			if(command.equalsIgnoreCase("HELLO")) {
				Main.println("[ListProtocol] Accepted HELLO; client version is " + payload);
				
				sendHello();
				state = STATE_HELLO;
				
				return true;
			}
		} else if(state == STATE_HELLO) {
			if(command.equalsIgnoreCase("GAMELIST")) {
				sendGamelist();
				return true;
			} else if(command.equalsIgnoreCase("DETAILS")) {
				try {
					int id = Integer.parseInt(payload);
					sendDetails(id);
					return true;
				} catch(NumberFormatException nfe) {}
			}
		}
		
		return false;
	}
}

package bnglist;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class FileRetriever extends Retriever {
	//retrieves games from a log file
	
	public static final int STATE_TERMINATING = -1;
	public static final int STATE_TERMINATED = -2;
	public static final int STATE_INITIALIZED = 0;
	public static final int STATE_RUNNING = 1;

	int id;
	Games games;
	int state;
	
	//config
	File target; //log of games to input from
	int sleepTime; //time to sleep after each check of target file, in ms
	
	public FileRetriever(int id, Games games) {
		this.id = id;
		this.games = games;
		state = STATE_INITIALIZED;
		
		String targetString = Config.getString(getKey("file"), "bnglist.in");
		target = new File(targetString);
		sleepTime = Config.getInt(getKey("sleeptime"), 1000);
	}
	
	public void println(String message) {
		Main.println("[Retriever " + id + "] " + message);
	}
	
	public String getKey(String key) {
		return "retriever" + id + "_" + key;
	}
	
	public boolean terminate() {
		if(state == STATE_RUNNING) {
			state = STATE_TERMINATING;
		} else if(state == STATE_INITIALIZED) {
			state = STATE_TERMINATED;
		}
		
		return state == STATE_TERMINATED;
	}
	
	public void run() {
		if(state == STATE_INITIALIZED) {
			//todo: syncronization...  kind of pointless though
			state = STATE_RUNNING;
		}
		
		while(state == STATE_RUNNING) {
			if(target.exists()) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(target));
					String line;
					
					while((line = in.readLine()) != null) {
						games.addGame(line);
					}
					
					in.close();
				} catch(FileNotFoundException e) {
					println("target.exists() returned true but file not found thrown");
				} catch(IOException e) {
					println("error while reading from source " + target.getPath());
				}
				
				target.delete();
			}
			
			try {
				Thread.sleep(sleepTime);
			} catch(InterruptedException e) {}
		}
	}
}

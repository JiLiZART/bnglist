package bnglist;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import bnglist.list.ListServer;
import bnglist.worker.WorkerServer;

public class Main {
	public static String BNGLIST_VERSION = "bnglist 0 (http://code.google.com/p/bnglist/)";
	public static File logTarget = null;
	
	public static void main(String args[]) {
		println(BNGLIST_VERSION);
		String propertiesFile = "bnglist.cfg";
		
		if(args.length >= 1) {
			propertiesFile = args[0];
		}
		
		boolean result = Config.init(propertiesFile);
		if(!result) return; //fatal error
		
		println("[Main] Starting up");
		
		Games games = new Games();
		games.start();
		ArrayList<Retriever> retrievers = new ArrayList<Retriever>();
		
		//search for retrieverXX_ in config
		for(int i = -1; i < 16; i++) {
			String prefix = "retriever" + i + "_";
			if(i == -1) prefix = "retriever_";
			
			if(Config.containsKey(prefix + "type")) {
				String type = Config.getString(prefix + "type", "file");
				Main.println("[Main] Loading retriever " + i + ", type=" + type);
				
				if(type.equals("file")) {
					FileRetriever retriever = new FileRetriever(i, games);
					retrievers.add(retriever);
					retriever.start();
				}
			}
		}
		
		ListServer server = new ListServer(games);
		server.init();
		server.start();
		
		if(Config.getBoolean("worker", false)) {
			WorkerServer workerServer = new WorkerServer(games);
			workerServer.init();
			workerServer.start();
		}
	}
	
	public static void println(String message) {
		System.out.println(message);
		
		//output to file
		if(logTarget != null) {
			try {
				PrintWriter out = new PrintWriter(new FileWriter(logTarget));
				out.println(message);
				out.close();
			} catch(IOException ioe) {
				System.out.println("[Main] Output to " + logTarget + " failed; disabling");
				logTarget = null;
			}
		}
	}
}

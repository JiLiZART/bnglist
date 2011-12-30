package worker;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Main {
	public static String BNGLIST_VERSION = "bnglist-worker 0 (http://code.google.com/p/bnglist/)";
	public static File logTarget = null;
	
	public static void main(String args[]) {
		println(BNGLIST_VERSION);
		String propertiesFile = "worker.cfg";
		
		if(args.length >= 1) {
			propertiesFile = args[0];
		}
		
		boolean result = Config.init(propertiesFile);
		if(!result) return; //fatal error
		
		println("[Main] Starting up");
		
		WorkerClient client = new WorkerClient();
		client.init();
		client.start();
		
		ArrayList<BnetRealm> realms = new ArrayList<BnetRealm>();
		//search for bnet realms and their connections
		for(int i = -1; i < 16; i++) {
			String prefix = "realm" + i + "_";
			if(i == -1) prefix = "realm_";
			
			if(Config.containsKey(prefix + "name")) {
				BnetRealm realm = new BnetRealm(i, Config.getString(prefix + "name", null), client);
				realms.add(realm);
				
				for(int j = -1; j < 16; j++) {
					String conn = prefix + "bnet" + j;
					if(j == -1) conn = prefix + "bnet";
					
					if(Config.containsKey(conn)) {
						realm.createConnection(j);
					}
				}
			}
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

package worker;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class Config {
	public static boolean tft;
	public static String war3path;
	public static int hostport; //the port that should be sent in NETGAMEPORT
	
	static Properties properties;
	
	public static boolean init(String propertiesFile) {
		properties = new Properties();
		Main.println("[Config] Loading configuration file " + propertiesFile);
		
		try {
			properties.load(new FileInputStream(propertiesFile));
		} catch(FileNotFoundException e) {
			Main.println("[Config] Fatal error: could not find configuration file " + propertiesFile);
			return false;
		} catch(IOException e) {
			Main.println("[Config] Fatal error: error while reading from configuration file " + propertiesFile);
			return false;
		}
		
		tft = getBoolean("worker_tft", true);
		war3path = getString("worker_war3path", "/usr/lib/");
		
		if(war3path.charAt(war3path.length() - 1) != '/' || war3path.charAt(war3path.length() - 1) != '\\') {
			war3path += "/";
		}
		
		hostport = getInt("worker_hostport", 6112);
		
		return true;
	}
	
	public static String getString(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
	
	public static int getInt(String key, int defaultValue) {
		try {
			String result = properties.getProperty(key, null);
			
			if(result != null) {
				return Integer.parseInt(result);
			} else {
				return defaultValue;
			}
		} catch(NumberFormatException nfe) {
			Main.println("[Config] Warning: invalid integer for key " + key);
			return defaultValue;
		}
	}
	
	public static boolean getBoolean(String key, boolean defaultValue) {
		String result = properties.getProperty(key, null);
		
		if(result != null) {
			if(result.equals("true") || result.equals("1")) return true;
			else if(result.equals("false") || result.equals("0")) return false;
			else {
				Main.println("[Config] Warning: invalid boolean for key " + key);
				return defaultValue;
			}
		} else {
			return defaultValue;
		}
	}
	
	public static boolean containsKey(String key) {
		return properties.containsKey(key);
	}
}

package bnglist;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class Config {
	static Properties properties;
	
	public static boolean init(String propertiesFile) {
		properties = new Properties();
		Main.println("[Config] Loading configuration file " + propertiesFile);
		
		try {
			properties.load(new FileInputStream(propertiesFile));
			return true;
		} catch(FileNotFoundException e) {
			Main.println("[Config] Fatal error: could not find configuration file " + propertiesFile);
			return false;
		} catch(IOException e) {
			Main.println("[Config] Fatal error: error while reading from configuration file " + propertiesFile);
			return false;
		}
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

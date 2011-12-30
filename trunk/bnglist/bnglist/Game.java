package bnglist;

public class Game {
	String mapPath;
	String hostName;
	String ipString;
	int port;
	int hostCounter;
	int gameType;
	int mapFlags;
	int mapWidth;
	int mapHeight;
	int elapsedTime;
	int mapCRC;
	String gameName;
	
	long seenTime;
	
	private Game() {
		
	}
	
	public String getHashString() {
		return ipString + "|" + port;
	}
	
	public String getOverviewString() {
		return "GAMENAME " + gameName +
			";IP " + ipString +
			";PORT " + port +
			";MAP " + mapPath;
	}
	
	public String getDetailString() {
		return "GAMENAME " + gameName +
			"\nIP " + ipString +
			"\nPORT " + port +
			"\nMAP " + mapPath +
			"\nHOSTNAME " + hostName +
			"\nHOSTCOUNTER " + hostCounter +
			"\nGAMETYPE " + gameType + 
			"\nMAPFLAGS " + mapFlags +
			"\nMAPWIDTH " + mapWidth +
			"\nMAPHEIGHT " + mapHeight +
			"\nELAPSED " + elapsedTime +
			"\nCRC " + mapCRC +
			"\nEND";
	}
	
	public String toString() {
		return getDetailString();
	}
	
	public static Game getGame(String input, long seenTime) {
		try {
			Game game = new Game();
			
			String[] parts = input.split("\\|", 12);
			
			if(parts.length < 12) return null;
			
			game.mapPath = parts[0];
			game.hostName = parts[1];
			game.ipString = parts[2];
			game.port = Integer.parseInt(parts[3]);
			game.hostCounter = Integer.parseInt(parts[4]);
			game.gameType = Integer.parseInt(parts[5]);
			game.mapFlags = Integer.parseInt(parts[6]);
			game.mapWidth = Integer.parseInt(parts[7]);
			game.mapHeight = Integer.parseInt(parts[8]);
			game.elapsedTime = Integer.parseInt(parts[9]);
			game.mapCRC = Integer.parseInt(parts[10]);
			game.gameName = parts[11];
			
			game.seenTime = seenTime;
			
			return game;
		} catch(Exception e) {
			Main.println("[Game] Invalid input to parse: " + input);
			return null;
		}
	}
}

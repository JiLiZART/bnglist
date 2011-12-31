package bnglist;

import bnglist.util.Util;

public class Game {
	public String mapPath;
	public String hostName;
	public String ipString;
	public int port;
	public int hostCounter;
	public int gameType;
	public int mapFlags;
	public int mapWidth;
	public int mapHeight;
	public int elapsedTime;
	public int mapCRC;
	public String gameName;
	
	public long seenTime;
	public int realm;
	
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
			"\nHOSTCOUNTER " + Util.unsignedInt(hostCounter) +
			"\nGAMETYPE " + gameType + 
			"\nMAPFLAGS " + Util.unsignedInt(mapFlags) +
			"\nMAPWIDTH " + mapWidth +
			"\nMAPHEIGHT " + mapHeight +
			"\nELAPSED " + Util.unsignedInt(elapsedTime) +
			"\nCRC " + Util.unsignedInt(mapCRC) +
			"\nEND";
	}
	
	public String getCodeString() {
		return mapPath + "|" +
			hostName + "|" +
			ipString + "|" +
			port + "|" +
			Util.unsignedInt(hostCounter) + "|" +
			gameType + "|" +
			Util.unsignedInt(mapFlags) + "|" +
			mapWidth + "|" +
			mapHeight + "|" +
			Util.unsignedInt(elapsedTime) + "|" +
			Util.unsignedInt(mapCRC) + "|" +
			gameName;
	}
	
	public String toString() {
		return getDetailString();
	}
	
	public static Game getGame(String input, long seenTime) {
		try {
			Game game = new Game();
			
			String[] parts = input.split("\\|", 13);
			
			if(parts.length < 13) return null;
			
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
			game.realm = Integer.parseInt(parts[11]);
			game.gameName = parts[12];
			
			game.seenTime = seenTime;
			
			return game;
		} catch(Exception e) {
			Main.println("[Game] Invalid input to parse: " + input);
			return null;
		}
	}
}

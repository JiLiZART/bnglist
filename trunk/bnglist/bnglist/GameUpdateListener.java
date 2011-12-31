package bnglist;

public interface GameUpdateListener {
	public void gameAdded(int id, Game g);
	public void gameReplaced(int id, Game g); //replace means that the game changed but ipport identifier is the same
	public void gameDeleted(int id);
}

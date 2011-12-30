package bnglist;

public abstract class Retriever extends Thread {
	public abstract boolean terminate(); //returns whether or not termination is complete
	public abstract void run();
}

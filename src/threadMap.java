import java.util.Hashtable;
import java.util.ArrayList;

public class threadMap extends Thread {

	// attributes
	private String threadName;
	private String threadString;
	private Hashtable<String, ArrayList<Integer>> threadHashTable;
	
	// builder
	public threadMap(String threadName0) 
	{
		this.threadName = threadName0;
		this.threadHashTable = new Hashtable<String, ArrayList<Integer>>();
	}
	
	// methods	
	public String Getname() {
		return this.threadName;
	}
	
	public String GetString() {
		return this.threadString;
	}

	public void SetString(String threadString0) {
		this.threadString = threadString0;
	}	
	
	public void SetHashTable(Hashtable<String, ArrayList<Integer>> inputHashtable) {
		this.threadHashTable.putAll(inputHashtable);
	}	
	
	public Hashtable<String, ArrayList<Integer>> GetHashTable() {
		return this.threadHashTable;
	}	
	
	public void run() 
	{
		this.threadHashTable.putAll(MapReduce.mapping(this.GetString())); 
	}
}
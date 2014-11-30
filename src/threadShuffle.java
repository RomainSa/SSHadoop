import java.util.ArrayList;
import java.util.Hashtable;


public class threadShuffle extends Thread {

	// attributes
	private String threadName;
	private String[] threadMapKeys;
	private Hashtable<String,ArrayList<threadMap>> threadKeysDict;
	private Hashtable<String,ArrayList<Integer>> threadKeysValuesList;
	
	// builder
	public threadShuffle(String threadName0, String[] threadMapKey0, Hashtable<String,ArrayList<threadMap>> threadKeysDict0) 
	{
		this.threadName = threadName0;
		this.threadMapKeys = threadMapKey0;
		this.threadKeysDict = threadKeysDict0;
		this.threadKeysValuesList = new Hashtable<String,ArrayList<Integer>>();
	}
	
	// methods	
	public String GetName() {
		return this.threadName;
	}
	
	public String[] GetMapKeys() {
		return this.threadMapKeys;
	}

	public Hashtable<String,ArrayList<threadMap>> GetKeysDict() {
		return this.threadKeysDict;
	}
	
	public Hashtable<String,ArrayList<Integer>> GetKeysValuesList() {
		return this.threadKeysValuesList;
	}	
	
	public void run() 
	{
		for(String key : this.GetMapKeys())
			{
			this.threadKeysValuesList.put(key,MapReduce.shuffle(key,this.GetKeysDict())); 
			}
	}
}
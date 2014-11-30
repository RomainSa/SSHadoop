import java.util.Hashtable;

public class threadReduce extends Thread 
{

	// attributes
	private String threadName;
	private threadShuffle threadShuffle;
	private Hashtable<String, Integer> threadReducedKeyValue;
	
	// builder
	public threadReduce(String threadName0, threadShuffle threadShuffle0) 
	{
		this.threadName = threadName0;
		this.threadShuffle = threadShuffle0;
	}
	
	// methods	
	public String GetName() {
		return this.threadName;
	}
	
	public threadShuffle GetThreadShuffle() {
		return this.threadShuffle;
	}

	public Hashtable<String, Integer> GetReducedKeyValue() {
		return this.threadReducedKeyValue;
	}	
	
	public void run() 
	{
		this.threadReducedKeyValue = MapReduce.reducing(this.GetThreadShuffle()); 
	}	
}


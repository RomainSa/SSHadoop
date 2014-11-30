
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URISyntaxException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;


public class MapReduce {
	
	/* ---------------------------------------------------------------------------------------------------------
	 * 
	 * 												MAIN METHOD
	 * 
	 * -------------------------------------------------------------------------------------------------------*/
		
		public static void main(String[] args) 
			throws FileNotFoundException, IOException, InterruptedException, URISyntaxException,  SftpException, JSchException
		{
			////////////////////////////////////////////////////////////////////////////////////////////////////
			
			// PARAMETERS - Results will be written as 'output.txt' in input directory
			
			// input text file
			String inputFilePath = "/cal/homes/rsavidan/Tests/input10times.txt"; 
			// mode: 0 = local machine, 1 = distributed, using local network
			int version = 0;
			// working directory for distributed version
			String workingDirPath = "/cal/homes/rsavidan/SSHadoop_files";
			// if local, number of threads, if distributed, max number of nodes
			// /!\ number of nodes must be at least equal to number of keys in the input file
			int numberOfNodes = 4;
			// max time to wait till we get all mapped files (in seconds)
			int maxWaitTime = 600;
			
			// IMPORTANT : Files necessary for SSHadoop to run in folder 'parameters' (folder 'parameters' must be in same folder as input file)
			// - auth.txt file with login on first line, password on second line
			// - listMachines.txt with all machines adresses (1 machine address per line)
			// - map.jar (the jar file to be distributed)
			
			////////////////////////////////////////////////////////////////////////////////////////////////////
			
			System.out.println("------------------");
			System.out.println("Starting SSHadoop!");
			System.out.println("------------------");
			File inputFile = new File(inputFilePath);
			
			// loads previous version of 'listMachines' if it exists
			String[] listMachines = {""};
			if (version == 1) {
				listMachines = getMachinesList(inputFile.getParent() + "/parameters/listMachines.txt");
			}
			
			// splits inputs into various files depending on the number of nodes
			long startTime = System.currentTimeMillis();
			System.out.println("Split...");
			File inputDir = new File(inputFile.getParent()+"/inputSplit");
			deleteFolder(inputDir);
			inputDir.mkdir();
			inputFileSplit(inputFilePath, numberOfNodes);
			long endSplitTime = System.currentTimeMillis();
			
			// wait for split to finish
			String suffix = "";
			if (numberOfNodes <= 10) {
				suffix = ".part0";
			} else {
				suffix = ".part";
			}
			File lastSplitFile = new File(inputFile.getParent() + "/inputSplit/" + inputFile.getName() + suffix + (numberOfNodes-1));
			while (!lastSplitFile.exists()) {
				Thread.currentThread();
				Thread.sleep(100);
			}
			
			// MAP
			System.out.println("Map...");
			int numberOfThreads = numberOfNodes;
			ArrayList<threadMap> threadMapList = new ArrayList<threadMap>();
			ArrayList<threadMap> unaffectedThreadMapList = new ArrayList<threadMap>();
			if (version == 1) {
				// DISTRIBUTED MAP	
				// sends all inputs file to remote servers
				ArrayList<String> listMachinesUP = new ArrayList<String>(Arrays.asList(listMachines));
				ArrayList<String> listMappedFilesToGet = new ArrayList<String>();
				int splitNumber = 0;
				while (splitNumber < numberOfNodes) {
					// node name
					String machine = listMachinesUP.get(splitNumber % listMachinesUP.size());
					// output directory
					long currentTime = System.currentTimeMillis();
					File outputDir = new File(workingDirPath + "/" + machine + "-" + currentTime);
					deleteFolder(outputDir);
					outputDir.mkdir();
					if (splitNumber < 10) {
						suffix = ".part0";
					} else {
						suffix = ".part";
					}
					String splittedInputFilePath = inputDir.getAbsolutePath() + "/" + inputFile.getName() + suffix + splitNumber;
					listMappedFilesToGet.add(outputDir.getAbsolutePath() + "/" + inputFile.getName() + suffix + splitNumber + "MAPPED");
					
					// if file sending does not work then we try on others servers
					try {
						sendInputFiles.sendInputFile(splittedInputFilePath, machine, outputDir.getAbsolutePath());
						splitNumber += 1;
					} catch (Exception e) {
						listMachinesUP.remove(splitNumber);
						deleteFolder(outputDir);
						System.out.println("server: " + machine + " is down!");
					}
				}
				
				// set threadMaps
				for(int threadNumber = 0; threadNumber < numberOfThreads; threadNumber+=1) {
					threadMap threadMapX = new threadMap("threadMap" + threadNumber);
					threadMapList.add(threadMapX);
					if (threadNumber < 10) {
						suffix = ".part0";
					} else {
						suffix = ".part";
					}
					threadMapX.SetString(inputDir.getAbsolutePath() + "/" + inputFile.getName() + suffix + threadNumber);
				}
				
				// tries to read from mapped files
				unaffectedThreadMapList = new ArrayList<threadMap>(threadMapList);
				long startDistributedMapTime = System.currentTimeMillis();
				while ((System.currentTimeMillis()-startDistributedMapTime)<1000*maxWaitTime && !listMappedFilesToGet.isEmpty()) {
					ArrayList<String> listMappedFilesGot = new ArrayList<String>();
					for (String fileToGet : listMappedFilesToGet) {
						try {
							Thread.sleep(100);
							unaffectedThreadMapList.get(0).SetHashTable(readFromMappedFile(fileToGet));
							unaffectedThreadMapList.remove(0);
							listMappedFilesGot.add(fileToGet);
						} catch (Exception e) {
							// nothing
						}
					}
					// erase from tiles to get all files that have been retrieved
					for (String fileGot : listMappedFilesGot) {
						listMappedFilesToGet.remove(fileGot);					
					}
				}
				
				// erase working directory files
				File dir = new File(workingDirPath);
				purgeDirectory(dir);

				
			} else {
				// LOCAL MAP file by launching a thread for each part

				for(int threadNumber = 0; threadNumber< numberOfThreads; threadNumber+=1) {
					threadMap threadMapX = new threadMap("threadMap" + threadNumber);
					threadMapList.add(threadMapX);
					if (threadNumber < 10) {
						suffix = ".part0";
					} else {
						suffix = ".part";
					}
					threadMapX.SetString(inputDir.getAbsolutePath() + "/" + inputFile.getName() + suffix + threadNumber);
				}
				
				for(threadMap thread : threadMapList) {
					thread.start();
				}
				
				for(threadMap thread : threadMapList) {
					thread.join();
				}	
			}
			long endMapTime = System.currentTimeMillis();
			
			// creates dict between UMx and threads
			System.out.println("dict...");
			Hashtable<Hashtable<String,ArrayList<Integer>>,threadMap> UMxMachines = new Hashtable<Hashtable<String,ArrayList<Integer>>,threadMap>();
			for(threadMap threadMapX : threadMapList) {
				UMxMachines.put(threadMapX.GetHashTable(),threadMapX);
			}
			
			// creates key/UMx names dict
			Hashtable<String,ArrayList<threadMap>> keysUMx = new Hashtable<String,ArrayList<threadMap>>();		
			for(threadMap threadMapX : threadMapList) {
				addKeys(threadMapX,keysUMx);
			}
			long endDictTime = System.currentTimeMillis();
			
			// SHUFFLE
			System.out.println("Shuffle...");
			ArrayList<threadShuffle> threadShuffleList = new ArrayList<threadShuffle>();
			for(int threadNumber = 0; threadNumber < numberOfThreads; threadNumber+=1) {
				// here we use a partition of the key set per node
				threadShuffle threadShuffleX = new threadShuffle("threadShuffle" + threadNumber, Arrays.copyOfRange(keysUMx.keySet().toArray(new String[keysUMx.size()]),keysUMx.size()/numberOfThreads*threadNumber,Math.min(keysUMx.size(),keysUMx.size()/numberOfThreads*(threadNumber+1))), keysUMx);
				threadShuffleList.add(threadShuffleX);
				threadShuffleX.start();
			}
			
			// wait for all threads to finish
			for(threadShuffle thread : threadShuffleList)
			{
				thread.join();
			}
			long endShuffleTime = System.currentTimeMillis();
			
			// REDUCE
			System.out.println("Reduce...");
			ArrayList<threadReduce> threadReduceList = new ArrayList<threadReduce>();
			Hashtable<String, Integer> resultMR = new Hashtable<String, Integer>();
			
			for(int threadNumber = 0; threadNumber < numberOfThreads; threadNumber+=1) {
				threadReduce threadReduceX = new threadReduce("threadReduce" + threadNumber, threadShuffleList.get(threadNumber));			
				threadReduceList.add(threadReduceX);
				threadReduceX.start();
			}
			
			for(threadReduce thread : threadReduceList) {
				thread.join();
				for (String key : thread.GetReducedKeyValue().keySet())
				{
					resultMR.put(key,thread.GetReducedKeyValue().get(key));
				}
				
			}
			long endReduceTime = System.currentTimeMillis();

			// writes result into an 'output.txt' file
			File file = new File(inputFile.getParent() + "/output.txt");
			if (!file.exists()) 
			{
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for(String key : resultMR.keySet())
			{
				bw.write(key + " " + resultMR.get(key));
				bw.newLine();
			}
			bw.close();
			long endTime = System.currentTimeMillis();
			
			// displays elapsed time for each part of the process
			System.out.println("---------------------");		
			System.out.println("SPLIT: " + (endSplitTime-startTime)/1000 + " sec (" + Math.round((double)(endSplitTime-startTime)/(endTime-startTime)*100) + "%)");
			System.out.println("MAP: " + (endMapTime-endSplitTime)/1000 + " sec (" + Math.round((double)(endMapTime-endSplitTime)/(endTime-startTime)*100) + "%)");
			System.out.println("DICT: " + (endDictTime-endMapTime)/1000 + " sec (" + Math.round((double)(endDictTime-endMapTime)/(endTime-startTime)*100) + "%)");
			System.out.println("SHUFFLE: " + (endShuffleTime-endDictTime)/1000 + " sec (" + Math.round((double)(endShuffleTime-endDictTime)/(endTime-startTime)*100) + "%)");
			System.out.println("REDUCE: " + (endReduceTime-endShuffleTime)/1000 + " sec (" + Math.round((double)(endReduceTime-endShuffleTime)/(endTime-startTime)*100) + "%)");
			System.out.println("OUTPUT: " + (endTime-endReduceTime)/1000 + " sec (" + Math.round((double)(endTime-endReduceTime)/(endTime-startTime)*100) + "%)");
			System.out.println("---------------------");
			System.out.println("TOTAL: " + (endTime-startTime)/1000 + " sec");
			
		}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Shavadoop methods
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// delete all files in a directory
	public static void purgeDirectory(File dir) {
	    for (File file: dir.listFiles()) {
	        if (file.isDirectory()) purgeDirectory(file);
	        file.delete();
	    }
	}
		
	// gets the server list from ARP table
	public static String[] getMachinesList(String inputFilePath) throws FileNotFoundException, IOException {
		// reads from input file
		BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
		StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        
        // puts result into ArrayList and return it
        String resultString = sb.toString();
        br.close();	
        String[] resultStringArray = resultString.split("\\s+");
        return resultStringArray;
	}
	
	// deletes a folder
	public static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) {
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folder.delete();
	}
	
	// splits input file into n output files
	public static void inputFileSplit(String inputFilePath, int numberOfOutputFiles) throws FileNotFoundException, IOException, InterruptedException
	{	
		File inputFile = new File(inputFilePath);
		String cmd = "split -n l/" + numberOfOutputFiles + " -a2 -d " + inputFilePath + " " + inputFile.getParent() + "/inputSplit/" + inputFile.getName() + ".part";
		Process p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
	}
	
	// reads from input and puts result into an ArrayList (each element is a word)
	public static String[] fileToWords(String inputFilePath)
		throws FileNotFoundException, IOException
	{	
		// reads from file 'fileToLoad'
		BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
		StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        
        // puts result into list and return it
        String resultString = sb.toString();
        br.close();	
        String[] resultStringArray = resultString.split("\\s+");
        return resultStringArray;
	}
	
	// MAP: transforms input file into HashTable with key = word and values = 1
	public static Hashtable<String, ArrayList<Integer>> mapping(String filePath) 
		{
		Hashtable<String, ArrayList<Integer>> result = new Hashtable<String, ArrayList<Integer>>();
		try
		{
			// splits file per word (=key) and assign 1 (=value)
			String[] words = fileToWords(filePath);
			
			// puts list into a HashTable with all values = 1
			for (String word : words) 
				{
				word = word.toLowerCase();
				if(result.containsKey(word))
					{
					ArrayList<Integer> value = result.get(word);
					value.add(1);
					result.put(word, value);
					}
				else
					{
					ArrayList<Integer> value = new ArrayList<Integer>();
					value.add(1);
					result.put(word, value);
					}
				}
			return result;
		}
		catch (FileNotFoundException e1)
		{
			e1.printStackTrace();
			return result;
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return result;
		}
	}

	// SHUFFLING: 
	public static ArrayList<Integer> shuffle(String mapKey, Hashtable<String,ArrayList<threadMap>> keysUMx) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		// for each thread corresponding to the key (resolved thanks to keysUMx), we get the corresponding values and add it to an ArrayList
		for(threadMap thread : keysUMx.get(mapKey))
		{
			Hashtable<String,ArrayList<Integer>> UMx = thread.GetHashTable();
			ArrayList<Integer> values = UMx.get(mapKey);
			for(Integer value : values)
			{
				result.add(value);
			}
		}
		return result;
	}

	// REDUCE: sum all values for a given shuffle result
	public static Hashtable<String, Integer> reducing(threadShuffle thread) {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();
		String[] keys = thread.GetMapKeys();
				
		// sum all values for each key
		for (String key : keys)
		{
			int valueSum = 0;
			for(Integer value :	thread.GetKeysValuesList().get(key))
			{
				valueSum+=value;
			}
			result.put(key,valueSum);
		}
		
		return result;
	}
	
	// add keys of the map process to an HashTable
	public static void addKeys(threadMap myThread, Hashtable<String,ArrayList<threadMap>> myTable)
	{
		Hashtable<String,ArrayList<Integer>> UMx = myThread.GetHashTable();
		
		// puts each key of the UMx into 'myTable' as <key=UMX.key,value=ThreadName=>
		for(String key : UMx.keySet()) 
		{
			threadMap value = myThread;
			// if Hashtable does not have key, we add it with a new ArrayList<String>
			if(myTable.containsKey(key))
			{
				myTable.get(key).add(value); // add value(UMX name) to ArrayList
			}
			// if Hashtable does have key, we add an element to the ArrayList<String>
			else
			{
				// creates ArrayList<String> and add value to it
				ArrayList<threadMap> valuesList = new ArrayList<threadMap>();
				valuesList.add(value);
				// adds  key/value
				myTable.put(key,valuesList);
			}
		}
	}
	
	// add keys of the map process to an HashTable
	public static Hashtable<String,ArrayList<Integer>> readFromMappedFile(String mappedFilePath) throws IOException, InterruptedException {
		
		Hashtable<String,ArrayList<Integer>> result = new Hashtable<String,ArrayList<Integer>>();
		
		// reads from file
		BufferedReader br = new BufferedReader(new FileReader(mappedFilePath));
        String line = br.readLine();
        while (line != null) {
            // reads first element of line
            String key = "";
            key = line.replace("[","").replace("]","").replace(","," ").split(" ")[0];
            // reads second element
            ArrayList<Integer> value = new ArrayList<Integer>();
    	    String[] valSplit = line.replace("[","").replace("]","").replace(","," ").split(" ");
    	    for (String element : valSplit) {
    	    	try {
    	    		value.add(Integer.parseInt(element));
    	    	} catch (Exception e) {
    	    	}
    	    }
            result.put(key,value);
            line = br.readLine();
        }
        
        br.close();	
		return result;		
	}
}

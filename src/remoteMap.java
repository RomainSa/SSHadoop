
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.io.FileNotFoundException;


public class remoteMap {

	public static void writeFile(String outputFilePath, String message) throws IOException {
			// writes result into an 'output.txt' file
			File file = new File(outputFilePath);
			if (!file.exists()) 
			{
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(message);
			//bw.newLine();
			bw.close();
	}
	
	public static String[] readFromDetailsFile(String filePath) throws FileNotFoundException, IOException {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
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
	
	public static void main(String[] args) 
			throws URISyntaxException, IOException, InterruptedException {

		// current file location (including name and extension)
		File jarFile = new File(remoteMap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			
		// get files on current folder (one is map.jar, the other is the file to map)
		File folder = new File(jarFile.getParent());
		File[] listOfFiles = folder.listFiles();
		File fileToMap = listOfFiles[0];
		if (listOfFiles[0].getName().equals("map.jar")) {
			fileToMap = listOfFiles[1];
		}
		
		// launch map on current node
		threadMap threadMapX = new threadMap("threadMap");
		threadMapX.SetString(fileToMap.getAbsolutePath());
		threadMapX.start();
		threadMapX.join();
		
		// saves mapped table to file
		Hashtable<String, ArrayList<Integer>> resultTable;
		resultTable = threadMapX.GetHashTable();
		File file = new File(jarFile.getParent() + "/" + fileToMap.getName() + "MAPPED");
		if (!file.exists()) 
		{
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		for(String key : resultTable.keySet()) {
			bw.write(key + " " + resultTable.get(key));
			bw.newLine();
		}
		bw.close();
	}
}

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.io.FileNotFoundException;

import java.io.FileInputStream; 

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class sendInputFiles {
	
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
	
	public static String[] readFromAuthFile(String filePath) throws IOException {
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
	
	public static void sendInputFile(String inputFilePath, String destinationNode, String destinationDir) 
			throws URISyntaxException, IOException, InterruptedException, SftpException, JSchException {
		
		// parameters folder location
		File inputFile = new File(inputFilePath);
		String paramFolderPath = inputFile.getParent();
		paramFolderPath = (new File(paramFolderPath)).getParent() + "/parameters";
		
		// launch map on current node
		threadMap threadMapX = new threadMap("threadMap");
		threadMapX.SetString(inputFile.getAbsolutePath());
		threadMapX.start();
		threadMapX.join();
				
		// send files through JSCH

		// read login/password from file
		String[] auth = readFromAuthFile(paramFolderPath + "/auth.txt");
		String login = auth[0];
		String password = auth[1];
		
		// auth parameters/initialisation
		String SFTPHOST = destinationNode;
		int    SFTPPORT = 22;
		String SFTPUSER = login;
		String SFTPPASS = password;
		String SFTPWORKINGDIR = destinationDir;
		String INPUTFILETOTRANSFER = inputFilePath;
		String JARFILETOTRANSFER = paramFolderPath + "/map.jar";
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		 
		// open session
        JSch jsch = new JSch();
        session = jsch.getSession(SFTPUSER,SFTPHOST,SFTPPORT);
        session.setPassword(SFTPPASS);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        channel = session.openChannel("sftp");
        channel.connect();
        channelSftp = (ChannelSftp)channel;
        channelSftp.cd(SFTPWORKINGDIR);
        
        // sends files
        File f2 = new File(INPUTFILETOTRANSFER);
        File f3 = new File(JARFILETOTRANSFER);
        channelSftp.put(new FileInputStream(f2), f2.getName());
        channelSftp.put(new FileInputStream(f3), f3.getName());
		
        // launch jar execution
        ChannelExec channelExec =(ChannelExec) session.openChannel("exec");
	    channelExec.setCommand("java -jar " + destinationDir + "/map.jar");
	    channelExec.connect();
	    channelSftp.disconnect();
        channel.disconnect();
        channelExec.disconnect();
        session.disconnect();
        
	}
}
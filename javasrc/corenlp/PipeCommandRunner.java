package corenlp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;

import util.Arr;
import util.BasicFileIO;
import util.JsonUtil;
import util.U;

/** 
 * TWO MODES OF OPERATION:
 * (1) stdin-based pipe control, output to temp files
 * (2) sockets
 * 
 * #1 is lame.
 * 
 * (1) Protocol for sockets: one line for input, one line for output.
 * 
 * command is a single line with two tab-separated fields:
 *     PARSEDOC \t "Hello world."
 * first field is the command name.  second field is the text of the document as a JSON string.
 * NO NEWLINES ALLOWED!  Most JSON libraries escape newlines to \n's so you should be safe.
 * 
 * Output is a big-ass JSON object, all in one line.
 * 
 * 
 * (2) Protocol for pipe/tempfiles:
 * 
 * intended to have stdin be a pipe, and not do anything on stdout. 
 * caller has to be smart about checking for the output file. sigh.
 * 
 * THE CALLER MUST FLUSH THE PIPE AFTER WRITING TO IT!
 * BECAUSE WE USE A BUFFERED READER HERE!
 * 
 * protocol:
 * 
 * STARTUP: once this program is ready to receive commands, it will create the file requested
 * and write a \0 NULL to it.
 * 
 * CLIENT: give one-line parsing command, specifying an output file. flush it.
 * 
 * THIS PROGRAM: writes to the output file:
 *   (1) the result as one big-ass JSON object
 *   (2) the NULL \0 character.
 *   
 *  so when this program is truly done, the last byte of the output file is \0.
 *  the client is responsible for waiting on this, i guess with a busy wait.
 *  yes, sockets seem like a better system, no?
 */
public class PipeCommandRunner {
	Parse parser;
	
	/** only used for server mode */
	int port = -1;
	
	public static void main(String[] args) throws Exception {
		boolean doServer = false;
		PipeCommandRunner runner = new PipeCommandRunner();
		if (args[0].equals("--server")) {
			doServer = true;
			runner.port = Integer.parseInt(args[1]);
			args = Arr.subArray(args, 2, args.length);
		}
		String mode = args[0];
		String reportReadyTempfile = args[1];
		
		runner.parser = new Parse();
		runner.parser.mode = Parse.modeFromString(mode);
		if (runner.parser.mode==null) throw new RuntimeException();
		runner.parser.setAnnotatorsFromMode();

		BasicFileIO.writeFile("\0", reportReadyTempfile);

		if (doServer) {
			runner.socketServerLoop();
		} else {
			runner.stdinLoop();
		}
	}
	
	void stdinLoop() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ( (line=reader.readLine()) != null) {
			String[] parts = line.split("\t");
			String command = parts[0];
			String outputfile = parts[1];
			String inputPayload = parts[2];

			JsonNode result = runCommand(command, inputPayload);
			
			Writer out = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(outputfile), "UTF-8"));
			out.write(result.toString());
			out.write('\0');
			out.close();
		}
	}
	
	JsonNode runCommand(String command, String inputPayload) throws Exception {
		switch (command) {
		case "PARSEDOC":
			JsonNode input = JsonUtil.parse(inputPayload);
			String text = input.asText();
			return parser.processTextDocument(text);
		case "CRASH":
			throw new IOException("fake error");
		default:
			throw new RuntimeException("bad command: " + command);
		}
	}
	
	/****** socket stuff ******/

	ServerSocket parseServer = null;
	
	void socketServerLoop() throws JsonGenerationException, JsonMappingException, IOException {
		// declare a server socket and a client socket for the server
		// declare an input and an output stream
		BufferedReader br;
		Socket clientSocket = null;
		String commandstr=null;
		
		initializeServer();

		// Create a socket object from the ServerSocket to listen and accept
		// connections.
		// Open input and output streams

		while (true) {
//			System.err.println("[Server] Waiting for Connection on Port: "+port);
			try {
				clientSocket = getSocketConnection();
				br = new BufferedReader(new InputStreamReader(new DataInputStream(clientSocket.getInputStream())));
				commandstr = br.readLine();

			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			String[] parts = commandstr.split("\t");
			String command = parts[0];
			String payload = parts[1];
			JsonNode result = null;
			try {
				result = runCommand(command,payload);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			// TODO: undefined behavior if >2GB return value ... which feels pretty possible.
			// using a long for length here for future-proofing,
			// but it doesn't help now since byte arrays have max length ~2e9 (Integer.MAX_VALUE or so)
			byte[] resultToReturn = JsonUtil.om.writeValueAsBytes(result);
			long resultLength = (long) resultToReturn.length;
			
			ByteBuffer bb = ByteBuffer.allocate(8);
			bb.putLong(0, resultLength);
			clientSocket.getOutputStream().write(bb.array());
			clientSocket.getOutputStream().write(resultToReturn);
		}
//		parseServer.close();
	}
	
	void initializeServer() {
		try {
			parseServer = new ServerSocket(port);
			System.err.println("[Server] Started socket server on port "+port);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	}

	Socket getSocketConnection() throws IOException {
		// could be smarter here and reuse same socket for multiple commands.
		Socket clientSocket = parseServer.accept();
//		System.err.println("Connection Accepted From: "+clientSocket.getInetAddress());
		return clientSocket;
		
	}
}

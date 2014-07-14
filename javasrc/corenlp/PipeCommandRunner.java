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
 * (1) sockets
 * (2) stdin-based pipe control, output to temp files.  this is lame.
 * 
 * == Protocol for sockets ==
 * 
 * Input is a single line with two tab-separated fields, ending with a newline:
 *     PARSEDOC \t "Hello world." \n
 * first field is the command name.  second field is the text of the document as a JSON string.
 * NO NEWLINES ALLOWED IN THE TEXT DATA!  Most JSON libraries escape newlines to \n's so you should be safe.
 * 
 * Output is 
 * 1. big-endian 8-byte integer describing how many bytes the reponse will be.
 * 2. a big-ass JSON object of that length.
 * 
 * For example:
 *  $ java -cp [blablabla] corenlp.PipeCommandRunner --server 1234 pos
 *  $ echo -e 'PARSEDOC\t"hello world."' | nc localhost 1234 | xxd
 *  The first few xxd lines are:
0000000: 0000 0000 0000 00b3 7b22 7365 6e74 656e  ........{"senten
0000010: 6365 7322 3a5b 7b22 706f 7322 3a5b 2255  ces":[{"pos":["U
0000020: 4822 2c22 4e4e 222c 222e 225d 2c22 746f  H","NN","."],"to

 * == Protocol for pipe/tempfiles ==
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
		runner.parser = new Parse();

		while (args.length > 1) {
			if (args[0].equals("--server")) {
				doServer = true;
				runner.port = Integer.parseInt(args[1]);
				args = Arr.subArray(args, 2, args.length);
			}
			else if (args[0].equals("--configfile")) {
				System.err.println("[Server] Using CoreNLP configuration file: " + args[1]);
				runner.parser.setConfigurationFromFile(args[1]);
				args = Arr.subArray(args, 2, args.length);
			}
			else if (args[0].equals("--mode")) {
				System.err.println("[Server] Using mode type: " + args[1]);
				runner.parser.mode = Parse.modeFromString(args[1]);
				if (runner.parser.mode==null) throw new RuntimeException("bad mode " + args[1]);
				runner.parser.setAnnotatorsFromMode();
				args = Arr.subArray(args, 2, args.length);
			}
		}

		if (doServer) {
			runner.socketServerLoop();
		} else {
			String reportReadyTempfile = args[0];
			BasicFileIO.writeFile("\0", reportReadyTempfile);
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
		case "PING":
			return JsonUtil.toJson("PONG");
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

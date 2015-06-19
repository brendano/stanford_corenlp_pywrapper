// vim:ts=4:noet
package corenlp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.management.RuntimeErrorException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;

import com.google.common.collect.Lists;

import util.Arr;
import util.BasicFileIO;
import util.JsonUtil;
import util.U;

/** 
 * == Protocol ==
 * 
 * this can go either over a socket or over stdin for the command, named pipe for ouput.  (well the output is jus a filename but presumably a named pipe makes the most sense)
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
 * SOCKETSERVER EXAMPLE
 * in one terminal start the server with e.g.
		java -cp "lib/*:/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*" corenlp.SocketServer --server 1234 --configdict '{"annotators": "tokenize, ssplit"}' 
 * In a second terminal send it the command
		echo -e 'PARSEDOC\t"hello world."' | nc localhost 1234 | xxd
 *  The first few xxd lines are:
0000000: 0000 0000 0000 00b3 7b22 7365 6e74 656e  ........{"senten
0000010: 6365 7322 3a5b 7b22 706f 7322 3a5b 2255  ces":[{"pos":["U
0000020: 4822 2c22 4e4e 222c 222e 225d 2c22 746f  H","NN","."],"to

 * note that xxd doesnt read as far as possible so this is an incomplete view.

 * PIPE OUTPUT EXAMPLE
mkfifo out
java -cp "lib/*:/home/sw/corenlp/stanford-corenlp-full-2015-04-20/*" corenlp.SocketServer --outpipe out --configdict '{"annotators": "tokenize, ssplit"}'
PARSEDOC	"hi there"

 * the last line was stdin typed into the process.  in the second terminal,
xxd < out
0000000: 0000 0000 0000 0046 7b22 7365 6e74 656e  .......F{"senten
0000010: 6365 7322 3a5b 7b22 746f 6b65 6e73 223a  ces":[{"tokens":
0000020: 5b22 6869 222c 2274 6865 7265 225d 2c22  ["hi","there"],"
0000030: 6368 6172 5f6f 6666 7365 7473 223a 5b5b  char_offsets":[[

 */
public class SocketServer {
	JsonPipeline parser;
	boolean doSocketServer = false;
	boolean doNamedPipes = false;

	ServerSocket parseServer = null;
	int port = -1;
	String outpipeFilename;
	
	public static void main(String[] args) throws Exception {
		SocketServer runner = new SocketServer();
		runner.parser = new JsonPipeline();

		while (args.length > 1) {
			if (args[0].equals("--server")) {
				runner.doSocketServer = true;
				runner.port = Integer.parseInt(args[1]);
				args = Arr.subArray(args, 2, args.length);
			}
			else if (args[0].equals("--outpipe")) {
				runner.doNamedPipes = true;
				runner.outpipeFilename = args[1];
				args = Arr.subArray(args, 2, args.length);
			}
			else if (args[0].equals("--configfile")) {
				log("Using CoreNLP configuration file: " + args[1]);
				runner.parser.setConfigurationFromFile(args[1]);
				args = Arr.subArray(args, 2, args.length);
			}
			else if (args[0].equals("--configdict")) {
				JsonNode propsAsJson = JsonUtil.parse(args[1]);
				for (String key : Lists.newArrayList(propsAsJson.getFieldNames())) {
					String value = propsAsJson.get(key).asText();
					runner.parser.props.setProperty(key, value);
				}
				args = Arr.subArray(args, 2, args.length);
			}
			else {
				throw new RuntimeException("don't know option: " + args[0]);
			}
		}
		runner.parser.initializeCorenlpPipeline();
		log("CoreNLP pipeline initialized.");
		
		if (runner.doSocketServer) {
			runner.socketServerLoop();
		} else if (runner.doNamedPipes) {
			runner.namedpipeLoop();
		} else {
			throw new RuntimeException("no running mode selected");
		}
	}
	
	/****** generic functions for both socket and pipe operation ******/
	
	static void log(String message) {
		System.err.println("INFO:CoreNLP_JavaServer: " + message);
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
	
	void checkTimings() {
		if (parser.numDocs>0 && (
				parser.numDocs <= 10 || 
				(parser.numDocs <= 1000 && (parser.numDocs % 100 == 0)) ||
				(parser.numDocs % 1000 == 0)
				)) {
				double elapsed = (double) (System.currentTimeMillis() - parser.startMilli) / 1000.0;
				log(String.format("INPUT: %d documents, %d characters, %d tokens, %.1f char/doc, %.1f tok/doc RATES: %.3f doc/sec, %.1f tok/sec\n",
						parser.numDocs, parser.numChars, parser.numTokens,
						parser.numChars*1.0 / parser.numDocs,
						parser.numTokens*1.0 / parser.numDocs,
						parser.numDocs*1.0 / elapsed,
						parser.numTokens*1.0 / elapsed
						));
			}
	}
	

	JsonNode parseAndRunCommand(String commandstr) {
		if (commandstr == null) {
			return null;
		}
		String[] parts = commandstr.split("\t");
		if (parts.length != 2) {
			return null;
		}
		String command = parts[0];
		String payload = parts[1];
		JsonNode result = null;
		try {
			result = runCommand(command,payload);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	
	void writeResultToStream(JsonNode result, OutputStream outstream) throws IOException {
		// TODO: undefined behavior if >2GB return value ... which feels pretty possible.
		// using a long for length here for future-proofing,
		// but it doesn't help now since byte arrays have max length ~2e9 (Integer.MAX_VALUE or so)
		byte[] resultToReturn = JsonUtil.om.writeValueAsBytes(result);
		long resultLength = (long) resultToReturn.length;
		
		ByteBuffer bb = ByteBuffer.allocate(8);
		bb.putLong(0, resultLength);
		outstream.write(bb.array());
		outstream.write(resultToReturn);
	}
	
	/*******  socket server stuff   ***********/
	
	void initializeSocketServer() {
		try {
			parseServer = new ServerSocket(port);
			log("Started socket server on port "+port);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	}

	Socket getSocketConnection() throws IOException {
		// could be smarter here and reuse same socket for multiple commands.
//		log("sotimeout " + parseServer.getSoTimeout()); // seems to be 0 on both mac and linux, though linux is happy to return null and mac is not
		Socket clientSocket = parseServer.accept();
//		System.err.println("Connection Accepted From: "+clientSocket.getInetAddress());
		return clientSocket;
		
	}
	
	void socketServerLoop() throws JsonGenerationException, JsonMappingException, IOException {
		// declare a server socket and a client socket for the server
		// declare an input and an output stream
		BufferedReader br;
		Socket clientSocket = null;
		String commandstr=null;
		
		initializeSocketServer();

		while (true) {
//			log("Waiting for Connection on Port: "+port);
			commandstr = null;
			try {
				clientSocket = getSocketConnection();
				br = new BufferedReader(new InputStreamReader(new DataInputStream(clientSocket.getInputStream())));
				commandstr = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
//			log("COMMANDSTR " + commandstr);
			if (commandstr==null) {
				// on linux but not mac this seems to happen during startup
				// when the client isn't actually asking anything but on the server accept() seems to try to get something anyway
				continue;
			}
			JsonNode result = parseAndRunCommand(commandstr);
//			log("RESULT " + result);
			// result could be null.  let's just write it back since the client is waiting.
			writeResultToStream(result, clientSocket.getOutputStream());
			checkTimings();
		}
//		parseServer.close();
	}
	

	/***********  stdin/namedpipe loop  ***********/

	void namedpipeLoop() throws JsonGenerationException, JsonMappingException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		String inputline;
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(outpipeFilename, true));
//		OutputStream out = new FileOutputStream(outpipeFilename, true);
		log("Waiting for commands on stdin");
		while ( (inputline=reader.readLine()) != null) {
			JsonNode result = parseAndRunCommand(inputline);
			writeResultToStream(result, out);
			out.flush();
			checkTimings();
		}

	}

}

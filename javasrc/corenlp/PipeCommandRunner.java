package corenlp;

import java.io.*;

import org.codehaus.jackson.JsonNode;

import util.BasicFileIO;
import util.JsonUtil;

/** intended to have stdin be a pipe, and not do anything on stdout. 
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
 * RUNTIME:
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
	
	public static void main(String[] args) throws Exception {
		PipeCommandRunner runner = new PipeCommandRunner();
		String mode = args[0];
		String reportReadyTempfile = args[1];
		runner.parser = new Parse();
		runner.parser.mode = Parse.modeFromString(mode);
		if (runner.parser.mode==null) throw new RuntimeException();
		runner.parser.setAnnotatorsFromMode();
		BasicFileIO.writeFile("\0", reportReadyTempfile);
		runner.stdinLoop();
	}
	
	void stdinLoop() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ( (line=reader.readLine()) != null) {
//			System.out.println("LINE " + line);
			String[] parts = line.split("\t");
			String command = parts[0];
			String outputfile = parts[1];
			String inputPayload = parts[2];

			JsonNode result = null;
			
			switch (command) {
			case "PARSEDOC":
				JsonNode input = JsonUtil.parse(inputPayload);
				String text = input.asText();
				result = parser.processTextDocument(text);
				break;
			case "CRASH":
				throw new IOException("fake error");
			default:
				throw new RuntimeException("bad command: " + command);
			}
			
			Writer out = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(outputfile), "UTF-8"));
			out.write(result.toString());
			out.write('\0');
			out.close();
		}
	}
	
}

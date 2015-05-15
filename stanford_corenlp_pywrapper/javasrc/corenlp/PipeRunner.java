package corenlp;

import org.codehaus.jackson.JsonNode;

import util.Arr;
import util.BasicFileIO;
import util.JsonUtil;
import util.U;

/**
 * stdin/stdout commandline pipe mode that lightly wraps JsonPipeline.
 * 
 * INPUT: one line per document.
 *  	docid \t TextAsJsonStringOrObjectWithTextField 
 *  OUTPUT: as JSON, one doc per line ("jdoc").
 *    docid \t {sentences: [ {sentobj}, {sentobj}, ... ]}
 *  where each sentobj is
 *    {tokens: [...], char_offsets: [...], ....}
 *
 */
public class PipeRunner {
	ProcessingMode mode;
	JsonPipeline parse;
	
	static enum InputFormat {
		DETECT_JSON_VARIANT,
		RAW_TEXT
	};

	/** the pre-baked processing modes, that define annotators and outputs. */
	static enum ProcessingMode {
		NOMODE,
		SSPLIT,
		POS,
		NER,
		PARSE,
		NERPARSE;
	}
	static ProcessingMode modeFromString(String _mode) {
		return 
			_mode.equals("nomode") ? ProcessingMode.NOMODE :
			_mode.equals("ssplit") ? ProcessingMode.SSPLIT :
			_mode.equals("pos") ? ProcessingMode.POS :
			_mode.equals("ner") ? ProcessingMode.NER :
			_mode.equals("parse") ? ProcessingMode.PARSE :
			_mode.equals("nerparse") ? ProcessingMode.NERPARSE :
			null;
	}
	

	static void usage() {
		U.p("corenlp.Parse [options] \n" +
				"Processes document texts on and outputs NLP-annotated versions.\n" +
				"Both input and output formats are one document per line.\n" +
				"\n" +
				"Input format can be either\n" +
				"  one column:   TextField\n" +
				"  two columns:  docid \\t TextField\n" +
				"Where TextField could be either\n" +
				"  * a JSON string, or\n" +
				"  * a JSON object with field 'text'.\n" +
				"--raw-input  allows the text field to be raw text, interpreted as UTF-8 encoded.\n" +
				"Note that JSON strings can be preferable, since they can contain any type of whitespace.\n" +
				"\n" +
				"In all cases, the output mode is two-column: docid \\t NLPInfoAsJson\n" +
				"");
		System.exit(1);
	}

	public void runStdinStdout(InputFormat inputFormat) {
		for (String line : BasicFileIO.STDIN_LINES) {
			System.err.print(".");
			
			String[] parts = line.split("\t");
			String docid, doctext;
			JsonNode payload = null;
			if (inputFormat == InputFormat.DETECT_JSON_VARIANT) {
				payload =JsonUtil.parse(parts[parts.length-1]);
				doctext = 
						payload.isTextual() ? payload.asText() :
							payload.has("text") ? payload.get("text").asText() :
								null;
			}
			else if (inputFormat == InputFormat.RAW_TEXT) {
				doctext = parts[parts.length-1];
			}
			else { throw new RuntimeException("wtf"); }

			docid = parts.length >= 2 ? parts[0] :
				payload !=null && payload.has("docid") ? payload.get("docid").getTextValue() :
					"doc" + parse.numDocs;

				assert docid != null : "inconsistent 'docid' key";
				if (doctext == null) throw new RuntimeException("Couldn't interpret JSON payload: should be string, or else object with a 'text' field.");

				JsonNode outDoc = parse.processTextDocument(doctext);
				U.pf("%s\t%s\n", docid, JsonUtil.toJson(outDoc));
		}
		
		double elapsedSec = 1.0*(System.currentTimeMillis() - parse.startMilli) / 1000;
		System.err.print("\n");
		System.err.printf("%d docs, %d tokens, %.1f tok/sec, %.1f byte/sec\n", parse.numDocs, parse.numTokens, parse.numTokens*1.0/elapsedSec, parse.numChars*1.0/elapsedSec);
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			usage();
		}
		InputFormat inputFormat = InputFormat.DETECT_JSON_VARIANT;

		while (args.length > 1) {
			String flag = args[0];
			if (flag.equals("--raw-input")) {
				inputFormat = InputFormat.RAW_TEXT;
				args = Arr.subArray(args, 1, args.length);
			}
			else { throw new RuntimeException("bad flag: " + flag); }
		}
		
		
		throw new RuntimeException("TODO need to handle mode parsing; in the meantime this is broken");
		
//		PipeRunner runner = new PipeRunner();
//		String _mode = args[0];
//		ProcessingMode mode = modeFromString(_mode);
//		if (runner.mode==null) {
//			U.pf("Bad mode '%s' ... to disable a mode, use 'nomode'\n", _mode);
//			usage();
//		}
//		runner.runStdinStdout(inputFormat);
	}
	


}

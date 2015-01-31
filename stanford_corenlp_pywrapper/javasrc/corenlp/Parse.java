package corenlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jackson.JsonNode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import util.Arr;
import util.BasicFileIO;
import util.JsonUtil;
import util.U;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
// paths for stanford 3.2.0.  before that, it's e.s.nlp.trees.semgraph.SemanticGraph
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

/** 
 * Commandline runner for CoreNLP with JSON output.
 * 
 * INPUT: one line per document.
 *  	docid \t TextAsJsonStringOrObjectWithTextField 
 *  
 *  OUTPUT: as JSON, one doc per line ("jdoc").
 *    docid \t {sentences: [ {sentobj}, {sentobj}, ... ]}
 *    
 *  where each sentobj is
 *    {tokens: [...], char_offsets: [...], ....}
 *    
 *  TODO: no coref yet, will be an 'entities' key in the document's json object.
 *  
 *  see usage()
 */
public class Parse {

	StanfordCoreNLP pipeline;
	ProcessingMode mode;

	int numTokens = 0;
	int numDocs = 0;
	long startMilli = System.currentTimeMillis();
	
	public Parse() {
	}

	static enum ProcessingMode {
		SSPLIT,
		POS,
		NER,
		JUSTPARSE,
		MEDPARSE,
		FULLPARSE;
	}

	static enum InputFormat {
		DETECT_JSON_VARIANT,
		RAW_TEXT
	};

	static void usage() {
		U.p("corenlp.Parse [options] OUTPUTMODE\n" +
				"Processes document texts on and outputs NLP-annotated versions.\n" +
				"Both input and output formats are one document per line.\n" +
				"\n" +
				"You must supply an OUTPUTMODE, which is one of:\n" +
				"  ssplit:     tokenization and sentence splitting (included in all subsequent ones)\n" +
				"  pos:        POS (and lemmas)\n" +
				"  ner:        POS and NER (and lemmas)\n" +
				"  justparse:  minimal parsing, just POS and trees.  (No NER, no lemmas, no deps)\n" +
				"  medparse:   parsing with POS, lemmas, and dependencies (no NER)\n" +
				"  fullparse:  parsing with NER, POS, lemmas, depenencies.\n" +
				"              This is the maximum processing short of coreference.\n" +
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
	
	static ProcessingMode modeFromString(String _mode) {
		return 
			_mode.equals("ssplit") ? ProcessingMode.SSPLIT :
			_mode.equals("pos") ? ProcessingMode.POS :
			_mode.equals("ner") ? ProcessingMode.NER :
			_mode.equals("justparse") ? ProcessingMode.JUSTPARSE :
			_mode.equals("medparse") ? ProcessingMode.MEDPARSE :
			_mode.equals("fullparse") ? ProcessingMode.FULLPARSE :
			null;
	}

	static void setAnnotators(Properties props, ProcessingMode mode) {
		if (mode==ProcessingMode.SSPLIT) {
			props.put("annotators", "tokenize, ssplit");
		}
		else if (mode==ProcessingMode.POS) {
			props.put("annotators", "tokenize, ssplit, pos, lemma");
		}
		else if (mode==ProcessingMode.NER) {
			props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		} 
		else if (mode==ProcessingMode.JUSTPARSE || mode==ProcessingMode.MEDPARSE) {
			props.put("annotators", "tokenize, ssplit, pos, parse");			
		}
		else if (mode==ProcessingMode.FULLPARSE) {
			props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
		} 
		else {
			assert false : "bad mode";
		}
	}
	static void addTokenBasics(Map<String,Object> sent_info, CoreMap sentence) {
		List<List<Integer>> tokenSpans = Lists.newArrayList();
		List<String> tokenTexts = Lists.newArrayList();
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			List<Integer> span = Lists.newArrayList(token.beginPosition(), token.endPosition());
			tokenSpans.add(span);
			tokenTexts.add(token.value());
		}
		sent_info.put("tokens", (Object) tokenTexts);
		sent_info.put("char_offsets", (Object) tokenSpans);
	}
	static void addTokenAnno(Map<String,Object> sent_info, CoreMap sentence,
			String keyname, Class annoClass) {
		List<String> tokenAnnos = Lists.newArrayList();
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			tokenAnnos.add(token.getString(annoClass));
		}
		sent_info.put(keyname, (Object) tokenAnnos);
	}
	static void addParseTree(Map<String,Object> sent_info, CoreMap sentence) {
		sent_info.put("parse", sentence.get(TreeCoreAnnotations.TreeAnnotation.class).toString());
	}
	@SuppressWarnings("rawtypes")
	static void addDepsCC(Map<String,Object> sent_info, CoreMap sentence) {
		SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		List deps = jsonFriendlyDeps(dependencies);
		sent_info.put("deps_cc", deps);
	}
	@SuppressWarnings("rawtypes")
	static void addDepsBasic(Map<String,Object> sent_info, CoreMap sentence) {
		SemanticGraph dependencies = sentence.get(BasicDependenciesAnnotation.class);
		List deps = jsonFriendlyDeps(dependencies);
		sent_info.put("deps_basic", deps);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static List jsonFriendlyDeps(SemanticGraph dependencies) {
		List deps = new ArrayList();
		// Since the dependencies are for each sentence, we obtain the root
		// and add it to the list of dependency triples.
		// The method is explained in the following link:
		// http://stackoverflow.com/questions/16300056/stanford-core-nlp-missing-roots
                List deptriple;
                try {
                    IndexedWord root = dependencies.getFirstRoot();
                    deptriple = Lists.newArrayList(
                                    "root",
                                    -1,
                                    root.index() - 1);
                    deps.add(deptriple);
                } catch (Exception e) {
                    // This can happen: https://github.com/stanfordnlp/CoreNLP/issues/55
                }
                                
		for (SemanticGraphEdge e : dependencies.edgeIterable()) {
			deptriple = Lists.newArrayList(
				   e.getRelation().toString(), 
				   e.getGovernor().index() - 1,
				   e.getDependent().index() - 1);
			deps.add(deptriple);
		}
		return deps;
	}
	
	public void setConfigurationFromFile(String iniPropertiesFilename) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(iniPropertiesFilename));
		pipeline = new StanfordCoreNLP(props);
	}
	
	public void setAnnotatorsFromMode() {
		Properties props = new Properties();
		setAnnotators(props, mode);
		//	    props.setProperty("tokenize.whitespace", "true");
		//	    props.setProperty("ssplit.eolonly", "true");
		pipeline = new StanfordCoreNLP(props);
	}

	public void runStdinStdout(InputFormat inputFormat) {
		setAnnotatorsFromMode();

		for (String line : BasicFileIO.STDIN_LINES) {
			numDocs++; System.err.print(".");
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
					"doc" + numDocs;

				assert docid != null : "inconsistent 'docid' key";
				if (doctext == null) throw new RuntimeException("Couldn't interpret JSON payload: should be string, or else object with a 'text' field.");

				JsonNode outDoc = processTextDocument(doctext);
				U.pf("%s\t%s\n", docid, JsonUtil.toJson(outDoc));
		}
		
		double elapsedSec = 1.0*(System.currentTimeMillis() - startMilli) / 1000;
		System.err.print("\n");
		System.err.printf("%d docs, %d tokens, %.1f tok/sec\n", numDocs, numTokens, numTokens*1.0/elapsedSec);
	}

	/** runs the corenlp pipeline with all options, and returns all results as a JSON object. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	JsonNode processTextDocument(String doctext) {
		Annotation document = new Annotation(doctext);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		List<Map> outSentences = Lists.newArrayList();

		for(CoreMap sentence: sentences) {
			Map<String,Object> sent_info = Maps.newHashMap();
			addTokenBasics(sent_info, sentence);
			numTokens += ((List) sent_info.get("tokens")).size();
			switch(mode) {
			case SSPLIT:
				break;
			case POS:
				addTokenAnno(sent_info,sentence, "pos", PartOfSpeechAnnotation.class);
				addTokenAnno(sent_info,sentence, "lemmas", LemmaAnnotation.class);
				break;
			case JUSTPARSE:
				addTokenAnno(sent_info, sentence, "pos", PartOfSpeechAnnotation.class);
				addParseTree(sent_info,sentence);
				break;
			case MEDPARSE:
				addTokenAnno(sent_info, sentence, "pos", PartOfSpeechAnnotation.class);
				addTokenAnno(sent_info,sentence, "lemmas", LemmaAnnotation.class);
				addParseTree(sent_info,sentence);
				addDepsCC(sent_info,sentence);
				addDepsBasic(sent_info,sentence);
				break;
			case NER:
				addTokenAnno(sent_info, sentence, "pos", PartOfSpeechAnnotation.class);
				addTokenAnno(sent_info,sentence, "lemmas", LemmaAnnotation.class);
				addTokenAnno(sent_info, sentence, "ner", NamedEntityTagAnnotation.class);
				addTokenAnno(sent_info, sentence, "normner", NormalizedNamedEntityTagAnnotation.class);
				break;
			case FULLPARSE:
				addTokenAnno(sent_info, sentence, "pos", PartOfSpeechAnnotation.class);
				addTokenAnno(sent_info,sentence, "lemmas", LemmaAnnotation.class);
				addTokenAnno(sent_info, sentence, "ner", NamedEntityTagAnnotation.class);
				addTokenAnno(sent_info, sentence, "normner", NormalizedNamedEntityTagAnnotation.class);
				addParseTree(sent_info,sentence);
				addDepsCC(sent_info,sentence);
				addDepsBasic(sent_info,sentence);
				break;
			}
			outSentences.add(sent_info);
		}

		Map outDoc = new ImmutableMap.Builder()
		//	        	.put("text", doctext)
		.put("sentences", outSentences)
		.build();
		return JsonUtil.toJson(outDoc);
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
		
		Parse runner = new Parse();
		
		String _mode = args[0];
		runner.mode = modeFromString(_mode);
		if (runner.mode==null) {
			U.pf("Bad mode '%s'\n", _mode);
			usage();
		}
		
		runner.runStdinStdout(inputFormat);
	}
	
}

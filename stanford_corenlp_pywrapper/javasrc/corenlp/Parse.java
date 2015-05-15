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
 * A wrapper around a CoreNLP object that set up annotators, and can turn annotations into JSON.
 * 
 * Also includes a commandline mode that's a stdin/stdout filter.  Not used for server wrapper operation.
 * INPUT: one line per document.
 *  	docid \t TextAsJsonStringOrObjectWithTextField 
 *  OUTPUT: as JSON, one doc per line ("jdoc").
 *    docid \t {sentences: [ {sentobj}, {sentobj}, ... ]}
 *  where each sentobj is
 *    {tokens: [...], char_offsets: [...], ....}
 *    
 *  TODO: no coref yet, will be an 'entities' key in the document's json object.
 */
public class Parse {

	StanfordCoreNLP pipeline;
	Properties props = new Properties();
	
	int numTokens = 0;
	int numDocs = 0;
	int numChars = 0;
	long startMilli = System.currentTimeMillis();
	
	public Parse() {
	}

	static enum InputFormat {
		DETECT_JSON_VARIANT,
		RAW_TEXT
	};

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
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		props.load(new FileInputStream(iniPropertiesFilename));
	}
	
	/** assume the properties object has been set */
	void initializeCorenlpPipeline() {
		pipeline = new StanfordCoreNLP(props);
	}

	/** annotator is a stanford corenlp notion.  */
	void addAnnoToSentenceObject(Map<String,Object> sent_info, CoreMap sentence, String annotator) {
		switch(annotator) {
		case "tokens":
			break;
		case "cleanxml":
			break;
		case "ssplit":
			break;
		case "pos":
			addTokenAnno(sent_info,sentence, "pos", PartOfSpeechAnnotation.class);
			break;
		case "lemmas":
			addTokenAnno(sent_info,sentence, "lemmas", LemmaAnnotation.class);
			break;
		case "parse":
			addParseTree(sent_info,sentence);
			addDepsCC(sent_info,sentence);
			addDepsBasic(sent_info,sentence);
			break;
		case "depparse":
			addDepsCC(sent_info,sentence);
			addDepsBasic(sent_info,sentence);
			break;
		case "ner":
			addTokenAnno(sent_info, sentence, "ner", NamedEntityTagAnnotation.class);
			addTokenAnno(sent_info, sentence, "normner", NormalizedNamedEntityTagAnnotation.class);
			break;
		default:
			throw new RuntimeException("don't know how to handle annotator " + annotator);
		}
	}

	String[] annotators() {
		String annotatorsAllstr = (String) props.get("annotators");
		return annotatorsAllstr.trim().split(",\\s*");
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
			for (String annotator : annotators()) {
				addAnnoToSentenceObject(sent_info, sentence, annotator);
			}
			outSentences.add(sent_info);
		}

		Map outDoc = new ImmutableMap.Builder()
		//	        	.put("text", doctext)
			.put("sentences", outSentences)
			// coref entities would go here too
			.build();
		return JsonUtil.toJson(outDoc);
	}


}

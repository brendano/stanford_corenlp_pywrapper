package corenlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jackson.JsonNode;

import util.JsonUtil;
import util.U;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EntityTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentenceIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
// paths for stanford 3.2.0.  before that, it's e.s.nlp.trees.semgraph.SemanticGraph
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

/** 
 * A wrapper around a CoreNLP Pipeline object that knows how to turn output annotations into JSON,
 * with 0-oriented indexing conventions.
 * 
 *  implementation: needs to mirror edu/stanford/nlp/pipeline/XMLOutputter.java somewhat
 */
public class JsonPipeline {

	StanfordCoreNLP pipeline;
	Properties props = new Properties();
	
	int numTokens = 0;
	int numDocs = 0;
	int numChars = 0;
	long startMilli = -1;
	
	public JsonPipeline() {
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
	static void addEntityMentions(Map<String,Object> sent_info, CoreMap sentence) {
        List<CoreMap> coreMentions = sentence.get(MentionsAnnotation.class);
        List<Map> jsonMentions = new ArrayList<>();
        /* trying to figure out the keys in each mention. here's a printout from one.
MENTION August 2014
class edu.stanford.nlp.ling.CoreAnnotations$TextAnnotation	August 2014
class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetBeginAnnotation	3
class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetEndAnnotation	14
class edu.stanford.nlp.ling.CoreAnnotations$TokensAnnotation	[August-2, 2014-3]
class edu.stanford.nlp.ling.CoreAnnotations$TokenBeginAnnotation	1
class edu.stanford.nlp.ling.CoreAnnotations$TokenEndAnnotation	3
class edu.stanford.nlp.ling.CoreAnnotations$NamedEntityTagAnnotation	DATE
class edu.stanford.nlp.ling.CoreAnnotations$NormalizedNamedEntityTagAnnotation	2014-08
class edu.stanford.nlp.ling.CoreAnnotations$EntityTypeAnnotation	DATE
class edu.stanford.nlp.ling.CoreAnnotations$SentenceIndexAnnotation	0
class edu.stanford.nlp.time.TimeAnnotations$TimexAnnotation	<TIMEX3 tid="t1" type="DATE" value="2014-08">August 2014</TIMEX3>
MENTION Barack Obama
class edu.stanford.nlp.ling.CoreAnnotations$TextAnnotation	Barack Obama
class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetBeginAnnotation	17
class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetEndAnnotation	29
class edu.stanford.nlp.ling.CoreAnnotations$TokensAnnotation	[Barack-5, Obama-6]
class edu.stanford.nlp.ling.CoreAnnotations$TokenBeginAnnotation	4
class edu.stanford.nlp.ling.CoreAnnotations$TokenEndAnnotation	6
class edu.stanford.nlp.ling.CoreAnnotations$NamedEntityTagAnnotation	PERSON
class edu.stanford.nlp.ling.CoreAnnotations$EntityTypeAnnotation	PERSON
class edu.stanford.nlp.ling.CoreAnnotations$SentenceIndexAnnotation	0
MENTION Paris
class edu.stanford.nlp.ling.CoreAnnotations$TextAnnotation	Paris
class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetBeginAnnotation	66
class edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetEndAnnotation	71
class edu.stanford.nlp.ling.CoreAnnotations$TokensAnnotation	[Paris-5]
class edu.stanford.nlp.ling.CoreAnnotations$TokenBeginAnnotation	14
class edu.stanford.nlp.ling.CoreAnnotations$TokenEndAnnotation	15
class edu.stanford.nlp.ling.CoreAnnotations$NamedEntityTagAnnotation	LOCATION
class edu.stanford.nlp.ling.CoreAnnotations$EntityTypeAnnotation	LOCATION
class edu.stanford.nlp.ling.CoreAnnotations$SentenceIndexAnnotation	1
         */
        for (CoreMap mention : coreMentions) {
//            U.p("MENTION " + mention);
//        	for (Class k : mention.keySet()) {
//        		U.pf("%s\t%s\n", k, mention.get(k));
//        	}
            Map m = new HashMap<String, Object>();
            m.put("tokspan", Lists.newArrayList(
            		mention.get(TokenBeginAnnotation.class).intValue(),
            		mention.get(TokenEndAnnotation.class).intValue()));
            m.put("charspan", Lists.newArrayList(
            		mention.get(CharacterOffsetBeginAnnotation.class).intValue(),
            		mention.get(CharacterOffsetEndAnnotation.class).intValue()));
            m.put("sentence", mention.get(SentenceIndexAnnotation.class).intValue());
            String entityType = mention.get(EntityTypeAnnotation.class);
            m.put("type", entityType);
            if (mention.containsKey(NormalizedNamedEntityTagAnnotation.class)) {
            	m.put("normalized", mention.get(NormalizedNamedEntityTagAnnotation.class));
            }
            if (mention.containsKey(TimexAnnotation.class)) {
            	m.put("timex_xml", mention.get(TimexAnnotation.class).toString());
            }
            jsonMentions.add(m);
        }
        sent_info.put("entitymentions", jsonMentions);
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

	List getCorefInfo(Annotation doc) {
		Map<Integer, CorefChain> corefChains = doc.get(CorefChainAnnotation.class);
//		List<CoreMap> sentences = doc.get(SentencesAnnotation.class);
		List entities = new ArrayList();
		for (CorefChain chain : corefChains.values()) {
			List mentions = new ArrayList();
			CorefChain.CorefMention representative = chain.getRepresentativeMention();
			for (CorefChain.CorefMention corement : chain.getMentionsInTextualOrder()) {
				Map outment = new HashMap();
				outment.put("sentence", corement.sentNum-1);
				outment.put("tokspan_in_sentence", Lists.newArrayList(
								corement.startIndex-1, corement.endIndex-1));
				outment.put("head",corement.headIndex-1);
				outment.put("gender", corement.gender.toString());
				outment.put("animacy", corement.animacy.toString());
				outment.put("number", corement.number.toString());
				outment.put("mentiontype", corement.mentionType.toString());
				outment.put("mentionid", corement.mentionID);
				if (representative!=null && corement.mentionID==representative.mentionID) {
					outment.put("representative", true);
				}
				mentions.add(outment);
			}
			Map entity = ImmutableMap.builder()
					.put("mentions", mentions)
					.put("entityid", chain.getChainID())
					.build();
			entities.add(entity);
		}
		return entities;
	}
	/** annotator is a stanford corenlp notion.  */
	void addAnnoToSentenceObject(Map<String,Object> sent_info, CoreMap sentence, String annotator) {
		switch(annotator) {
		case "tokenize":
		case "cleanxml":
		case "ssplit":
			break;
		case "pos":
			addTokenAnno(sent_info,sentence, "pos", PartOfSpeechAnnotation.class);
			break;
		case "lemma":
			addTokenAnno(sent_info,sentence, "lemmas", LemmaAnnotation.class);
			break;
		case "ner":
			addTokenAnno(sent_info, sentence, "ner", NamedEntityTagAnnotation.class);
			addTokenAnno(sent_info, sentence, "normner", NormalizedNamedEntityTagAnnotation.class);
			break;
		case "regexner":
			addTokenAnno(sent_info, sentence, "ner", NamedEntityTagAnnotation.class);
			break;
		case "sentiment": throw new RuntimeException("TODO");
		case "truecase": throw new RuntimeException("TODO");
		case "parse":
			addParseTree(sent_info,sentence);
			addDepsCC(sent_info,sentence);
			addDepsBasic(sent_info,sentence);
			break;
		case "depparse":
			addDepsCC(sent_info,sentence);
			addDepsBasic(sent_info,sentence);
			break;
		case "dcoref":
			break;
		case "relation": throw new RuntimeException("TODO");
		case "natlog": throw new RuntimeException("TODO");
		case "quote": throw new RuntimeException("TODO");
		case "entitymentions":
			addEntityMentions(sent_info, sentence);
			break;
		default:
			throw new RuntimeException("don't know how to handle annotator " + annotator);
		}
	}

	String[] annotators() {
		String annotatorsAllstr = (String) props.get("annotators");
		if (annotatorsAllstr==null || annotatorsAllstr.trim().isEmpty()) {
			throw new RuntimeException("'annotators' property seems to not be set");
		}
		return annotatorsAllstr.trim().split(",\\s*");
	}
	
	/** runs the corenlp pipeline with all options, and returns all results as a JSON object. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	JsonNode processTextDocument(String doctext) {
		if (startMilli==-1)  startMilli = System.currentTimeMillis();
		numDocs++;
		numChars += doctext.length();

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


		ImmutableMap.Builder b = new ImmutableMap.Builder();
//		b.put("text", doctext);
		b.put("sentences", outSentences);
		
		if (Lists.newArrayList(annotators()).contains("dcoref")) {
			List outCoref = getCorefInfo(document);
			b.put("entities", outCoref);
		}
		Map outDoc = b.build();
		return JsonUtil.toJson(outDoc);
	}


}

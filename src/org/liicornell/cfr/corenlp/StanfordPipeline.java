package org.liicornell.cfr.corenlp;

import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefClusterIdAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class StanfordPipeline {
	private static StanfordPipeline instance;

	public final StanfordCoreNLP pipeline;
	public final GrammaticalStructureFactory gsf;
	public final LexicalizedParser lp;

	public static synchronized StanfordPipeline getInstance() {
		if (instance == null) {
			System.out.println("Initializing NLP pipeline...");
			instance = new StanfordPipeline();
			System.out.println("Done.");
		}
		return instance;
	}

	private StanfordPipeline() {
		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, parse, lemma, ner, dcoref");
		pipeline = new StanfordCoreNLP(props);
		lp = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		gsf = tlp.grammaticalStructureFactory();
	}

	public String resolvePronouns(String text) {
		Annotation doc = pipeline.process(text);
		Map<Integer, CorefChain> graph = doc.get(CorefChainAnnotation.class);

		StringBuilder s = new StringBuilder();
		for (CoreLabel token : doc.get(TokensAnnotation.class)) {
			if (isPronoun(token)) {
				Integer clusterId = token.get(CorefClusterIdAnnotation.class);
				if (clusterId == null) {
					System.err.println("PRONOUN ERROR");
					s.append(token.originalText());
				} else {
					s.append(graph.get(clusterId).getRepresentativeMention().mentionSpan);
				}
			} else {
				s.append(token.originalText());
			}
			s.append(token.after());
		}

		return s.toString();
	}

	private static boolean isPronoun(CoreLabel token) {
		String pos = token.get(PartOfSpeechAnnotation.class);
		return pos.equals("PRP") || pos.equals("PRP$");
	}

}

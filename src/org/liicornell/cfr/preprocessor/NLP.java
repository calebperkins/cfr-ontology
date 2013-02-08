package org.liicornell.cfr.preprocessor;

import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefClusterIdAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NLP {
	private static NLP instance;

	private final StanfordCoreNLP pipeline;

	public static synchronized NLP getInstance() {
		if (instance == null) {
			System.out.println("Initializing NLP pipeline...");
			instance = new NLP();
		}
		return instance;
	}

	private NLP() {
		Properties props = new Properties();
		props.put("annotators",
				"tokenize, ssplit, pos, parse, lemma, ner, dcoref");
		pipeline = new StanfordCoreNLP(props);
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

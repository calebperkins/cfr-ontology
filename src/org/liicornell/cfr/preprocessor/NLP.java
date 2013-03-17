package org.liicornell.cfr.preprocessor;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefClusterIdAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class NLP {
	private static NLP instance;

	private final StanfordCoreNLP pipeline;
	private final GrammaticalStructureFactory gsf;
	private final LexicalizedParser lp;

	public static synchronized NLP getInstance() {
		if (instance == null) {
			System.out.println("Initializing NLP pipeline...");
			instance = new NLP();
			System.out.println("Done.");
		}
		return instance;
	}

	private NLP() {
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

	public Set<Triple> generateTriples(String text) {
		Set<Triple> triples = new HashSet<Triple>();

		Reader reader = new StringReader(text);
		for (List<HasWord> sentence : new DocumentPreprocessor(reader)) {
			if (sentence.size() > 70)
				continue;
			
			GrammaticalStructure gs = gsf.newGrammaticalStructure(lp
					.apply(sentence));
			// Collection<TreeGraphNode> nodes = gs.getNodes();
			
			new TripleGenerator(gs, triples).run();
		}
		return triples;
	}

	private static boolean isPronoun(CoreLabel token) {
		String pos = token.get(PartOfSpeechAnnotation.class);
		return pos.equals("PRP") || pos.equals("PRP$");
	}

}

package org.liicornell.cfr.nlp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.coref.DefaultLinker;
import opennlp.tools.coref.DiscourseEntity;
import opennlp.tools.coref.Linker;
import opennlp.tools.coref.LinkerMode;
import opennlp.tools.coref.mention.DefaultParse;
import opennlp.tools.coref.mention.Mention;
import opennlp.tools.coref.mention.MentionContext;
import opennlp.tools.coref.resolver.ResolverUtils;
import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class NLP {
	private static NLP instance;

	final static TokenizerModel tokenModel;
	final static SentenceModel sentenceModel;
	final static ParserModel parseModel;

	public final SentenceDetector sentenceDetector;
	public final Tokenizer tokenizer;
	public final Parser parser;
	public final Linker linker;

	static {
		try {
			// tokens
			InputStream modelIn = new FileInputStream(
					"/Users/caleb/Documents/LII/Workspace/WithHadoop/datasets/en-token.bin");
			tokenModel = new TokenizerModel(modelIn);
			modelIn.close();

			// sentence model
			modelIn = new FileInputStream("/Users/caleb/Documents/LII/Workspace/WithHadoop/datasets/en-sent.bin");
			sentenceModel = new SentenceModel(modelIn);
			modelIn.close();

			// parser
			modelIn = new FileInputStream(
					"/Users/caleb/Documents/LII/Workspace/WithHadoop/datasets/en-parser-chunking.bin");
			parseModel = new ParserModel(modelIn);
			modelIn.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static synchronized NLP getInstance() {
		if (instance == null) {
			System.out.println("Initializing NLP pipeline...");
			instance = new NLP();
			System.out.println("Done initializing.");
		}
		return instance;
	}

	private NLP() {
		sentenceDetector = new SentenceDetectorME(sentenceModel);
		tokenizer = new TokenizerME(tokenModel);
		parser = ParserFactory.create(parseModel);
		try {
			linker = new DefaultLinker("datasets/coref", LinkerMode.TEST);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private Parse parseSentence(final String text, final Span[] tokens) {
		final Parse p = new Parse(text,
		// a new span covering the entire text
				new Span(0, text.length()),
				// the label for the top if an incomplete node
				AbstractBottomUpParser.INC_NODE,
				// the probability of this parse...uhhh...?
				1,
				// the token index of the head of this parse
				0);

		for (int idx = 0; idx < tokens.length; idx++) {
			final Span tok = tokens[idx];
			// find the index of the current token
			final int start = tok.getStart();
			// flesh out the parse with token sub-parses
			p.insert(new Parse(text, new Span(start, start + tok.length()), AbstractBottomUpParser.TOK_NODE, 0, idx));
		}
		synchronized (parser) {
			return parser.parse(p);
		}

	}

	public DiscourseEntity[] findEntityMentions(final String[] sentences, final Span[][] tokens) {
		// tokens should correspond to sentences
		assert sentences.length == tokens.length;

		// list of document mentions
		final List<Mention> document = new ArrayList<Mention>();

		for (int i = 0; i < sentences.length; i++) {
			// generate the sentence parse tree
			final Parse parse = parseSentence(sentences[i], tokens[i]);

			final DefaultParse parseWrapper = new DefaultParse(parse, i);
			final Mention[] extents = linker.getMentionFinder().getMentions(parseWrapper);

			// Note: taken from TreebankParser source...
			for (int ei = 0, en = extents.length; ei < en; ei++) {
				// construct parses for mentions which don't have constituents
				if (extents[ei].getParse() == null) {
					// not sure how to get head index, but it doesn't seem to be
					// used at this point
					final Parse snp = new Parse(parse.getText(), extents[ei].getSpan(), "NML", 1.0, 0);
					parse.insert(snp);
					// setting a new Parse for the current extent
					extents[ei].setParse(new DefaultParse(snp, i));
				}
			}
			document.addAll(Arrays.asList(extents));
		}

		if (document.isEmpty()) {
			return new DiscourseEntity[0];
		}

		synchronized (linker) {
			return linker.getEntities(document.toArray(new Mention[0]));
		}

	}

	public String resolvePronouns(String text) {
		StringBuilder sb = new StringBuilder();

		// text = "Caleb likes dogs. Caleb thinks they are cute.";

		// text =
		// "Pierre Vinken, 61 years old, will join the board as a nonexecutive director Nov. 29. Mr. Vinken is chairman of Elsevier N.V., the Dutch publishing group. Rudolph Agnew, 55 years old and former chairman of Consolidated Gold Fields PLC, was named a director of this British industrial conglomerate.";
		String[] sentences;
		synchronized (sentenceDetector) {
			sentences = sentenceDetector.sentDetect(text);
		}
		Span[][] tokens = new Span[sentences.length][];
		synchronized (tokenizer) {
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = tokenizer.tokenizePos(sentences[i]);
			}
		}

		DiscourseEntity[] mentions = findEntityMentions(sentences, tokens);

		Map<Span, String> use = new HashMap<Span, String>();

		for (DiscourseEntity mention : mentions) {
			Iterator<MentionContext> x = mention.getMentions();
			String best = ResolverUtils.mentionString(x.next());
			while (x.hasNext()) {
				MentionContext context = x.next();
				if (context.getFirstTokenTag().equals("PRP")) {
					use.put(context.getSpan(), best);
				}
			}
		}

		for (int i = 0; i < tokens.length; i++) {
			for (int j = 0; j < tokens[i].length; j++) {
				Span q = tokens[i][j];
				CharSequence t = use.containsKey(q) ? use.get(q) : tokens[i][j].getCoveredText(sentences[i]);
				sb.append(t);
				sb.append(' ');
			}
		}

		return sb.toString();
	}

	public Set<Triple> generateTriples(String text) {
		Set<Triple> triples = new HashSet<Triple>();
		// TODO
		return triples;
	}

}

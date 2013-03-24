package org.liicornell.cfr.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import opennlp.tools.parser.Parse;
import opennlp.tools.util.Span;

public class TripleGenerator implements Runnable {
	private final Set<Triple> triples;
	private final String[] sentences;
	private final Span[][] tokens;

	public TripleGenerator(Set<Triple> triples, final String[] sentences, final Span[][] tokens) {
		this.triples = triples;
		this.sentences = sentences;
		this.tokens = tokens;
	}

	protected static Parse getType(Parse p, String type) {
		if (p == null)
			return null;
		
		if (p.getType().equals(type))
			return p;
		
		for (Parse kid : p.getChildren()) {
			Parse np = getType(kid, type);
			if (np != null)
				return np;
		}
		return null;
	}
	
	private static boolean isModifier(Parse p) {
		String t = p.getType();
		return t.equals("NN") || t.equals("JJ");
	}
	
	private void findNarrowerTriples(Parse np) {
		final int n = np.getChildCount();
		final Parse[] children = np.getChildren();
		Parse main = children[n-1];
		for (int i = 0; i < n - 1; i++) {
			Parse child = children[i];
			if (isModifier(child)) {
				Triple t = Triple.narrower(main.toString(), child.toString() + ' ' + main.toString());
				Triple tt = t.inversion();
				synchronized (triples) {
					triples.add(t);
					triples.add(tt);
				}
			}
		}
	}
	
	private static Parse collapse(Parse p) {
		for (Parse kid : p.getChildren()) {
			if (kid.getType().equals("NP"))
				return kid;
		}
		return p;
	}
	
	private void findRelatedTriples(Parse p) {
		Parse parent = p.getParent();
		List<Parse> related = new ArrayList<Parse>();
		for (Parse sibiling : parent.getChildren()) {
			sibiling = collapse(sibiling);
			if (isNoun(sibiling)) {
				related.add(sibiling);
			}
		}
		for (Parse a : related) {
			for (Parse b : related) {
				if (a.equals(b))
					continue;
				Triple t = Triple.related(a.toString(), b.toString());
				synchronized (triples) {
					triples.add(t);
				}
			}
		}
	}
	
	private static boolean isNoun(Parse node) {
		if (node == null)
			return false;
		return node.getType().startsWith("N");
	}
	
	private void traverse(Parse p) {		
		if (p.getType().equals("NP")) {
			findNarrowerTriples(p);
		} else if (p.getType().equals("CC")) {
			findRelatedTriples(p);
		}
		
		for (Parse child : p.getChildren()) {
			traverse(child);
		}
	}
	
//	private void getSubjectVerbObjects(Parse p) {
//		Parse s = getType(p, "S");
//		Parse subject = getType(s, "NP");
//		Parse vp = getType(s, "VP");
//		Parse verb = getType(vp, "VBZ");
//		Parse object = getType(vp, "NP");
//		
//		if (subject != null && verb != null && object != null) {
////			verbSubjects.put(verb, subject);
////			verbObjects.put(verb, object);
//			Triple t = makeTriple(subject, object, verb);
//			synchronized (triples) {
//				triples.add(t);
//			}
//		}
//		
//	}
	
//	private static String npToString(Parse np) {
//		StringBuilder sb = new StringBuilder();
//		for (Parse p : np.getChildren()) {
//			if (!p.getType().equals("DT")) {
//				sb.append(p.toString());
//				sb.append(' ');
//			}
//		}
//		return sb.toString();
//	}
//	
//	private static Triple makeTriple(Parse s, Parse o, Parse p) {
//		return Triple.lii(npToString(s), npToString(o), p.toString());
//	}
	
	private static void prune(Parse p) {
		int n = p.getChildCount();
		Parse[] children = p.getChildren();
		for (int i = 0; i < n; i++) {
			Parse c = children[i];
			if (c.getType().equals("DT")) {
				p.remove(i);
			}
		}
		
		for (Parse c : p.getChildren()) {
			prune(c);
		}
	}

	@Override
	public void run() {
		for (int i = 0; i < sentences.length; i++) {
			String sentence = sentences[i];
			Span[] spans = tokens[i];
			Parse[] parses = NLP.getInstance().parseSentence(sentence, spans, 1);
			
			for (Parse p : parses) {
				prune(p);
				p.show();
				traverse(p);
			}
		}

	}

}

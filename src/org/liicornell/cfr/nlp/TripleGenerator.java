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
				return collapse(kid);
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
		return node != null && node.getType().startsWith("N");
	}
	
	private static boolean isVerb(Parse node) {
		return node != null && node.getType().startsWith("V");
	}
	
	private static Parse getVerb(Parse root) {
		if (isVerb(root) && !root.getType().equals("VP"))
			return root;
		for (Parse c : root.getChildren()) {
			Parse v = getVerb(c);
			if (v != null)
				return v;
		}
		return null;
	}
	
	private void findSubjectObjectTriples(Parse p) {
		Parse parent = p.getParent();
		Parse s = getType(parent, "S");
		Parse subject = collapse(getType(s, "NP"));
		Parse vp = getType(s, "VP");
		Parse verb = getVerb(vp);
		Parse object = collapse(getType(vp, "NP"));
		
		if (subject != null && verb != null && object != null && !subject.equals(object)) {
			Triple t = Triple.lii(subject.toString(), object.toString(), verb.toString());
			synchronized (triples) {
				triples.add(t);
			}
		}
		
	}
	
	private void traverse(Parse p) {		
		if (p.getType().equals("NP")) {
			findNarrowerTriples(p);
		} else if (p.getType().equals("CC")) {
			findRelatedTriples(p);
		} else if (p.getType().equals("VP")) {
			findSubjectObjectTriples(p);
		}
		
		for (Parse child : p.getChildren()) {
			traverse(child);
		}
	}
	
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
			Parse[] parses = NLP.getInstance().parseSentence(sentence, spans, 2);
			
			for (Parse p : parses) {
				if (getType(p, "S") == null)
					continue;
				prune(p);
				p.show();
				traverse(p);
			}
		}

	}

}

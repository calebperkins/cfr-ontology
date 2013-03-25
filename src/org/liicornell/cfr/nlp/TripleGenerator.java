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
		return isNoun(p) || isAdjective(p);
	}

	private void findNarrowerTriples(Parse np) {
		final int n = np.getChildCount();
		final Parse[] children = np.getChildren();
		Parse main = children[n - 1];
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
		if (p == null)
			return null;
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
		return node != null && node.getType().startsWith("NN");
	}

	private static boolean isAdjective(Parse node) {
		return node != null && node.getType().startsWith("JJ");
	}

	private static Parse getFirstNoun(Parse root) {
		if (isNoun(root))
			return root;
		for (Parse c : root.getChildren()) {
			Parse v = getFirstNoun(c);
			if (v != null)
				return v;
		}
		return null;
	}

	private static Parse getFirstAdjective(Parse root) {
		if (isAdjective(root))
			return root;
		for (Parse c : root.getChildren()) {
			Parse v = getFirstAdjective(c);
			if (v != null)
				return v;
		}
		return null;
	}

	private static boolean isVerb(Parse node) {
		return node != null && node.getType().startsWith("VB");
	}

	private void traverse(Parse p) {
		if (p.getType().equals("NP")) {
			findNarrowerTriples(p);
		} else if (p.getType().equals("CC")) {
			findRelatedTriples(p);
		} else if (p.getType().equals("S")) {
			getSubjectVerbObjectTriples(p);
		}

		for (Parse child : p.getChildren()) {
			traverse(child);
		}
	}

	private static class Pair {
		public Parse tree;
		public int depth;

		public Pair(Parse p, int d) {
			tree = p;
			depth = d;
		}
	}

	private static Pair getDeepestVerb(Parse p) {
		Pair result = new Pair(p, 0);
		for (Parse c : p.getChildren()) {
			Pair childResult = getDeepestVerb(c);
			if (childResult.depth + 1 > result.depth && isVerb(childResult.tree)) {
				result.depth = 1 + childResult.depth;
				result.tree = childResult.tree;
			}
		}
		return result;
	}

	private void getSubjectVerbObjectTriples(Parse s) {
		Parse[] x = s.getChildren();
		Parse np = x[0];
		Parse vp = x[1];

		Parse subject = getSubject(np);
		if (subject == null)
			return;

		Parse verb = getDeepestVerb(vp).tree;
		if (verb == null)
			return;

		// get object, which are siblings of verb tree
		vp = verb.getParent();
		Parse object = null;
		for (Parse sibiling : vp.getChildren()) {
			String t = sibiling.getType();
			if (t.equals("PP") || t.equals("NP")) {
				object = getFirstNoun(sibiling);
				break;
			} else if (t.equals("ADJP")) {
				object = getFirstAdjective(sibiling);
				break;
			}
		}
		if (object == null)
			return;

		Triple t = Triple.lii(subject.toString(), object.toString(), verb.toString());
		synchronized (triples) {
			triples.add(t);
		}
	}

	private static Parse getSubject(Parse root) {
		if (isNoun(root))
			return root;
		for (Parse c : root.getChildren()) {
			Parse subject = getSubject(c);
			if (subject != null)
				return subject;
		}
		return null;
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
				if (!p.getChildren()[0].getType().equals("S"))
					continue;
				
				prune(p);
				p.show();
				traverse(p);
			}
		}

	}

}

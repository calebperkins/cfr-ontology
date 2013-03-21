package org.liicornell.cfr.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;

public class TripleGenerator {
	private final List<TypedDependency> tdl;
	private final Set<Triple> triples;
	private final Collection<TreeGraphNode> nodes;
	
//	private final Map<TreeGraphNode, List<String>> predicateSubPropertyMap = new HashMap<TreeGraphNode, List<String>>();
	private final Map<TreeGraphNode, List<String>> vocabMap = new HashMap<TreeGraphNode, List<String>>();
	private final Map<TreeGraphNode, List<TreeGraphNode>> nSubjMap = new HashMap<TreeGraphNode, List<TreeGraphNode>>();
	private final Map<TreeGraphNode, List<TreeGraphNode>> dObjMap = new HashMap<TreeGraphNode, List<TreeGraphNode>>();
	private final Map<TreeGraphNode, String> nnMap = new HashMap<TreeGraphNode, String>();
	private final Map<TreeGraphNode, TreeGraphNode> conjMap = new HashMap<TreeGraphNode, TreeGraphNode>();
	private final List<TreeGraphNode> negativeList = new ArrayList<TreeGraphNode>();

	private static Set<String> stopWords = new HashSet<String>();

	public TripleGenerator(GrammaticalStructure gs, Set<Triple> triples) {
		tdl = gs.typedDependenciesCCprocessed(true);
		removeRomanNumerals(tdl);
		
		nodes = gs.getNodes();

		this.triples = triples;
	}
	
	// Finds the noun after verb for finding object of nsubj(verb, noun)
	private TreeGraphNode findNounAfterVerb(TreeGraphNode verbNode) {
		boolean foundVerb = false;

		for (TreeGraphNode node : nodes) {
			if (node.equals(verbNode)) {
				foundVerb = true;
			}
			if (foundVerb == true && isNoun(node.label().tag())) {
				return node;
			}
		}
		return null;
	}

	// Generate conj and negation hash maps
	private void generateConjugateAndNegationMaps() {
		for (TypedDependency td : tdl) {
			if (td.reln().toString().contains("conj")) {
				conjMap.put(td.gov(), td.dep());
				conjMap.put(td.dep(), td.gov());
			}
			if (td.reln().toString().equals("neg")) {
				negativeList.add(td.gov());
			}
		}
	}

	public void run() {
		generateNNmap();
		generateAModNNAdvModTriples();
		generatePrepTriples();

		generateConjugateAndNegationMaps();
		generateConjTriples();

		generateHearstPatternTriples();

		// uses predicateSubPropertyMap, nSubjMap, dObjMap
		generateNSubjDObjHashMaps();
		generateNSubjDobj();
	}

	// Generate triples of the form nsubj, dObj
	private void generateNSubjDobj() {
		for (TreeGraphNode node : nSubjMap.keySet()) {
			if (dObjMap.containsKey(node)) {
				// Direct NSubj DObj typed dependency is present
				if (negativeList.contains(node)) {
					// If negation is attached to the verb
					for (TreeGraphNode snode : nSubjMap.get(node))
						for (TreeGraphNode onode : dObjMap.get(node)) {
							if (isNoun(onode.label().tag())) {
								// If Conj Hash Map contains node create triples
								// for conjugate as well
								if (conjMap.containsKey(snode)) {

									Triple triple = Triple.lii(
											getFullyQualifiedNoun(conjMap
													.get(snode)),
											getFullyQualifiedNoun(onode),
											"not "
													+ getFullyQualifiedVerb(node));
									triples.add(triple);

								}
								if (conjMap.containsKey(onode)) {
									Triple triple = Triple.lii(
											getFullyQualifiedNoun(snode),
											getFullyQualifiedNoun(conjMap
													.get(onode)),
											"not "
													+ getFullyQualifiedVerb(node));
									triples.add(triple);

								}
								Triple triple = Triple.lii(
										getFullyQualifiedNoun(snode),
										getFullyQualifiedNoun(onode), "not "
												+ getFullyQualifiedVerb(node));
								triples.add(triple);
							}
						}
				} else {
					for (TreeGraphNode snode : nSubjMap.get(node))
						for (TreeGraphNode onode : dObjMap.get(node)) {
							if (isNoun(onode.label().tag())) {
								if (conjMap.containsKey(snode)) {
									Triple triple = Triple.lii(
											getFullyQualifiedNoun(conjMap
													.get(snode)),
											getFullyQualifiedNoun(onode),
											getFullyQualifiedVerb(node));
									triples.add(triple);

								}
								if (conjMap.containsKey(onode)) {
									Triple triple = Triple.lii(
											getFullyQualifiedNoun(snode),
											getFullyQualifiedNoun(conjMap
													.get(onode)),
											getFullyQualifiedVerb(node));
									triples.add(triple);

								}
								Triple triple = Triple.lii(
										getFullyQualifiedNoun(snode),
										getFullyQualifiedNoun(onode),
										getFullyQualifiedVerb(node));
								triples.add(triple);
							}

						}
				}
			} else {
				// Direct DObj is not present, we need to find Dobj from the
				// sentence. That is find the first noun after the verb
				TreeGraphNode object = findNounAfterVerb(node);
				if (object == null) {
					continue;
				}
				// Check if it actually a noun. Get fully qualified subject,
				// object and verb in all cases.
				// If you find one noun after verb traverse till you find all
				// consecutive nouns since they are all applicable
				if (isNoun(object.nodeString())) {
					continue;
				}
				// TreeGraphNode objectNode = object;
				// String objectString = "";

				if (object != null) {
					if (negativeList.contains(node)) {
						// Negation associated with verb
						for (TreeGraphNode snode : nSubjMap.get(node)) {
							if (conjMap.containsKey(snode)) {
								String multipleObjects = getFullyQualifiedNounForObject(object);
								for (String fullyqualifiedObject : multipleObjects
										.split(",")) {
									Triple triple = Triple.lii(
											getFullyQualifiedNoun(conjMap
													.get(snode)),
											fullyqualifiedObject,
											"not "
													+ getFullyQualifiedVerb(node));
										triples.add(triple);
								}

							}
							String multipleObjects = getFullyQualifiedNounForObject(object);
							// If multiple objects are present then create
							// triples for each
							for (String fullyqualifiedObject : multipleObjects
									.split(",")) {
								Triple triple = Triple.lii(
										getFullyQualifiedNoun(snode),
										fullyqualifiedObject, "not "
												+ getFullyQualifiedVerb(node));
									triples.add(triple);
							}

						}
					} else {
						for (TreeGraphNode snode : nSubjMap.get(node)) {
							if (conjMap.containsKey(snode)) {
								String multipleObjects = getFullyQualifiedNounForObject(object);
								for (String fullyqualifiedObject : multipleObjects
										.split(",")) {
									Triple triple = Triple.lii(
											getFullyQualifiedNoun(conjMap
													.get(snode)),
											fullyqualifiedObject
													+ findObjectString(object),
											getFullyQualifiedVerb(node));
										triples.add(triple);
								}

							}
							String multipleObjects = getFullyQualifiedNounForObject(object);
							for (String fullyqualifiedObject : multipleObjects
									.split(",")) {
								Triple triple = Triple.lii(
										getFullyQualifiedNoun(snode),
										fullyqualifiedObject
												+ findObjectString(object),
										getFullyQualifiedVerb(node));
									triples.add(triple);
							}

						}
					}
				}
			}
		}
	}

	// FInd consecutive nouns after the first noun after the verb
	private String findObjectString(TreeGraphNode objectNode) {

		List<TreeGraphNode> nodeArrayList = new ArrayList<TreeGraphNode>();
		for (TreeGraphNode node : nodes) {
			nodeArrayList.add(node);
		}
		Collections.sort(nodeArrayList, new Comparator<TreeGraphNode>() {

			@Override
			public int compare(TreeGraphNode node1, TreeGraphNode node2) {
				try {
					Integer no1 = Integer
							.parseInt(node1.toString().split("-")[1]);
					Integer no2 = Integer
							.parseInt(node2.toString().split("-")[1]);
					return no1.compareTo(no2);
				} catch (Exception e) {
					// e.printStackTrace();
					return -1;
				}

			}
		});
		boolean foundObject = false;
		String objectSTring = "";
		for (TreeGraphNode node : nodeArrayList) {
			if (node.equals(objectNode)) {
				foundObject = true;
				continue;
			}
			if (foundObject == true
					&& (isNoun(node.label().tag()) || (isAdj(node.label().tag())))) {
				objectSTring += " " + node.nodeString();
			}
			if (foundObject == true
					&& !(isNoun(node.label().tag()) || (isAdj(node.label()
							.tag())))) {
				return objectSTring;
			}
		}
		return objectSTring;
	}

	private static String getFullyQualifiedVerb(TreeGraphNode node) {
		return node.nodeString();
	}

	// FInd all the qualifiers of noun in vocab hash map
	private String getFullyQualifiedNoun(TreeGraphNode subject) {
		if (vocabMap.containsKey(subject)) {
			Iterator<String> iterator = vocabMap.get(subject).iterator();
			String fullyQualifiedObject = iterator.next();
			while (iterator.hasNext()) {
				String fString = iterator.next();
				if (fString.length() > fullyQualifiedObject.length()) {
					fullyQualifiedObject = fString;
				}
			}
			return fullyQualifiedObject;
		} else {
			subject.nodeString();
		}
		return subject.nodeString();
	}

	private String getFullyQualifiedNounForObject(TreeGraphNode subject) {
		if (vocabMap.containsKey(subject)) {
			Iterator<String> iterator = vocabMap.get(subject).iterator();
			String fullyQualifiedObject = iterator.next();
			while (iterator.hasNext()) {
				fullyQualifiedObject += "," + iterator.next();
			}
			return fullyQualifiedObject;
		} else {
			subject.nodeString();
		}
		return subject.nodeString();
	}

	// Generate hash maps for nsubj dobj as well as create noun noun narrowers
	// in nsubj and nsubjpass
	private void generateNSubjDObjHashMaps() {
		for (TypedDependency td1 : tdl) {
			if (td1.reln().toString().equals("nsubj")
					|| td1.reln().toString().equals("nsubjpass")) {
				if (isNoun(td1.gov().label().tag())
						&& isNoun(td1.dep().label().tag())) {
					List<String> subjectList = vocabMap.get(td1.gov());
					List<String> objectList = vocabMap.get(td1.dep());
					if (subjectList == null) {
						subjectList = new ArrayList<String>();
						subjectList.add(td1.gov().nodeString());
					}
					if (objectList == null) {
						objectList = new ArrayList<String>();
						objectList.add(td1.dep().nodeString());
					}
					for (String s : subjectList) {
						for (String o : objectList) {
							Triple triple = Triple.narrower(s, o);
							triples.add(triple);
						}
					}
				} else if (isAdj(td1.gov().label().tag())
						&& isNoun(td1.dep().label().tag())) {
					List<String> subjectList = vocabMap.get(td1.gov());
					List<String> objectList = vocabMap.get(td1.dep());
					if (subjectList == null) {
						subjectList = new ArrayList<String>();
						subjectList.add(td1.gov().nodeString());
					}
					if (objectList == null) {
						objectList = new ArrayList<String>();
						objectList.add(td1.dep().nodeString());
					}
					for (String s : subjectList) {
						for (String o : objectList) {
							Triple triple = Triple.narrower(s, o);
							triples.add(triple); // was commented out for some reason
						}
					}
				}

				else {

					// Check for anaphora resolution
					if (isNoun(td1.dep().label().tag())) {
						if (nSubjMap.containsKey(td1.gov())) {
							nSubjMap.get(td1.gov()).add(td1.dep());
						} else {
							List<TreeGraphNode> nodeList = new ArrayList<TreeGraphNode>();
							nodeList.add(td1.dep());
							nSubjMap.put(td1.gov(), nodeList);
						}
					}
				}

			}
			if (td1.reln().toString().equals("dobj")) {
				if (dObjMap.containsKey(td1.gov())) {
					dObjMap.get(td1.gov()).add(td1.dep());
				} else {
					List<TreeGraphNode> nodeList = new ArrayList<TreeGraphNode>();
					nodeList.add(td1.dep());
					dObjMap.put(td1.gov(), nodeList);
				}
			}

		}

	}

	/**
	 * Locate hearst patterns using dependencies for "such as", "as", "including" and "like."
	 */
	private void generateHearstPatternTriples() {
		for (TypedDependency td : tdl) {
			if (td.reln().toString().equals("prep_as")
					|| td.reln().toString().equals("prep_such_as")
					|| td.reln().toString().equals("prep_including")
					|| td.reln().toString().equals("prep_like")
					|| td.reln().toString().equals("prepc_as")
					|| td.reln().toString().equals("prepc_such_as")
					|| td.reln().toString().equals("prepc_including")
					|| td.reln().toString().equals("prepc_like")) {
				Triple triple = Triple.narrower(td.gov().nodeString(), td.dep()
						.nodeString());
				triples.add(triple);
			}
		}

	}

	// Generate conj triple by skos:related
	private void generateConjTriples() {
		for (TreeGraphNode node : conjMap.keySet()) {
			TreeGraphNode objectNode = conjMap.get(node);
			if ((isNoun(node.label().tag()) && (isNoun(objectNode.label().tag())))
					|| (isVerb(node.label().tag()) && (isVerb(objectNode
							.label().tag())))
					|| (isAdj(node.label().tag()) && isAdj(objectNode.label()
							.tag()))) {
				List<String> subjectList = vocabMap.get(node);
				List<String> objectList = vocabMap.get(conjMap.get(node));
				if (subjectList == null) {
					subjectList = new ArrayList<String>();
					subjectList.add(node.nodeString());
				}
				if (objectList == null) {
					objectList = new ArrayList<String>();
					objectList.add(conjMap.get(node).nodeString());
				}
				for (String s : subjectList) {
					for (String o : objectList) {
						if (!(s.equals("other") || o.equals("other"))) {
							Triple triple = Triple.related(s, o);
							triples.add(triple);
						}

					}
				}
			}
		}

	}


	private void generatePrepTriples() {
		for (TypedDependency td : tdl) {
			if (td.reln().toString().contains("prep")) {
				// Filter herast patterns
				if (!(td.reln().toString().equals("prep_as") || td.reln().toString().equals("prep_such_as")
						|| td.reln().toString().equals("prep_including") || td.reln().toString().equals("prep_like")
						|| td.reln().toString().equals("prepc_as") || td.reln().toString().equals("prepc_such_as")
						|| td.reln().toString().equals("prepc_including") || td.reln().toString().equals("prepc_like"))) {
					if ((isNoun(td.gov().label().tag()) /*
														 * ||
														 * isVerb(td.gov().label
														 * ().tag())
														 */)
							&& (isNoun(td.dep().label().tag()) || isVerb(td.dep().label().tag()))) {
						if (td.reln().toString().split("_").length > 1) {
							// Find the prep
							String predicate = td.reln().toString().split("_")[1];
							List<String> subjectList = vocabMap.get(td.gov());
							List<String> objectList = vocabMap.get(td.dep());
							// FInd fully qualified subject and object. Append
							// object as subject prep object
							if (subjectList == null) {
								subjectList = new ArrayList<String>();
								subjectList.add(td.gov().nodeString());
							}
							if (objectList == null) {
								objectList = new ArrayList<String>();
								objectList.add(td.dep().nodeString());
							}
							// Remove double prepositions
							for (String s : subjectList) {
								for (String o : objectList) {
									if (s.split(" ").length >= 3 && o.split(" ").length > 3) {
										Triple triple = Triple.narrower(td.gov().nodeString(), td.gov().nodeString()
												+ " " + predicate + " " + td.dep().nodeString());
										triples.add(triple);

										Triple inverseTriple = Triple.related(td.dep().nodeString(), td.gov()
												.nodeString() + " " + predicate + " " + td.dep().nodeString());
										triples.add(inverseTriple);
									} else if (s.split(" ").length >= 3) {
										Triple triple = Triple.narrower(td.gov().nodeString(), td.gov().nodeString()
												+ " " + predicate + " " + o);
										triples.add(triple);
										Triple inverseTriple = Triple.related(o, td.gov().nodeString() + " "
												+ predicate + " " + o);

										triples.add(inverseTriple);

									} else if (o.split(" ").length >= 3) {
										Triple triple = Triple.narrower(s, s + " " + predicate + " "
												+ td.dep().nodeString());
										triples.add(triple);
										Triple inverseTriple = Triple.related(td.dep().nodeString(), s + " "
												+ predicate + " " + td.dep().nodeString());

										triples.add(inverseTriple);
									} else {
										Triple triple = Triple.narrower(s, s + " " + predicate + " " + o);
										triples.add(triple);
										Triple inverseTriple = Triple.related(o, s + " " + predicate + " " + o);

										triples.add(inverseTriple);

									}

								}
							}

							List<String> value = vocabMap.get(td.gov());
							if (value == null) {
								value = new ArrayList<String>();
							}

							value.add(td.gov().nodeString() + " " + predicate + " " + td.dep().nodeString());
							vocabMap.put(td.gov(), value);
						}
					}
				}
			}
		}
	}

	// Add to triple store and hashmap.Find fully qualified using nnHashMap
	private void addToTripleStoreAndHashMap(TreeGraphNode noun, TreeGraphNode adjective) {

		String nounString = nnMap.containsKey(noun) ? nnMap.get(noun) : noun.nodeString();

		Triple triple = Triple.narrower(nounString, adjective.nodeString() + " " + nounString);
		triples.add(triple);

		List<String> value;
		if (vocabMap.containsKey(noun)) {
			value = vocabMap.get(noun);
		} else {
			value = new ArrayList<String>();
			vocabMap.put(noun, value);
		}
		value.add(adjective.nodeString() + " " + nounString);
	}

	// Create skos:narrower for amods. Filter stop words
	private void generateAModNNAdvModTriples() {
		for (TypedDependency td : tdl) {
			if (td.reln().equals(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER)) {
				if (!stopWords.contains(td.dep().nodeString())) {
					if ((isAdj(td.dep().label().tag()) || isNoun(td.dep().label().tag()) || isVerb(td.dep().label()
							.tag()))
							&& (isAdj(td.gov().label().tag()) || isNoun(td.gov().label().tag()) || isVerb(td.gov()
									.label().tag())))
						addToTripleStoreAndHashMap(td.gov(), td.dep());
				}
			}
		}
	}

	private static boolean isNoun(String value) {
		if (value == null) {
			return false;
		}
		return value.equals("NN") || value.equals("NNS") || value.equals("NNPS") || value.equals("NNP");
	}

	private static boolean isVerb(String value) {
		return value.equals("VB") || value.equals("VBD") || value.equals("VBG") || value.equals("VBP")
				|| value.equals("VBN") || value.equals("VBZ");
	}

	private static boolean isAdj(String value) {
		if (value == null) {
			return false;
		}
		return value.equals("JJ") || value.equals("JJR") || value.equals("JJS");
	}

	private void generateNNmap() {
		for (TypedDependency td : tdl) {
			if (!td.reln().equals(EnglishGrammaticalRelations.NOUN_COMPOUND_MODIFIER)
					|| td.dep().nodeString().equals("other"))
				continue;

			TreeGraphNode noun = td.gov();
			TreeGraphNode modifier = td.dep();
			Triple triple = Triple.narrower(noun.nodeString(), modifier.nodeString() + " " + noun.nodeString());
			triples.add(triple);

			String modifierString = modifier.nodeString() + " " + noun.nodeString();
			if (nnMap.containsKey(noun))
				nnMap.put(noun, modifier.nodeString() + " " + nnMap.get(noun));
			else
				nnMap.put(noun, modifierString);
			List<String> value = vocabMap.get(noun);
			if (value == null) {
				value = new ArrayList<String>();
				value.add(modifier.nodeString() + " " + noun.nodeString());
				// vocabHashMap.put(noun, value);
			} else {
				String nounModifierString = "";
				for (String s : value) {
					nounModifierString = s;
				}
				nounModifierString = nounModifierString.replace(noun.nodeString(), modifierString);
				value.add(nounModifierString);
				triple = Triple.narrower(modifierString, nounModifierString);
				triples.add(triple);
			}
		}
	}

	private static void removeRomanNumerals(List<TypedDependency> tdl) {
		Iterator<TypedDependency> itr = tdl.iterator();
		while (itr.hasNext()) {
			TypedDependency td = itr.next();
			if (td.dep().nodeString().length() == 1 || td.gov().nodeString().length() == 1
					|| td.dep().nodeString().matches("(^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$)")
					|| td.gov().nodeString().matches("(^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$)")) {
				itr.remove();
			}
		}
	}

}

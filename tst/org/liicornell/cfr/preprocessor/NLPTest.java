package org.liicornell.cfr.preprocessor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.liicornell.cfr.nlp.NLP;
import org.liicornell.cfr.nlp.Triple;
import org.liicornell.cfr.runner.RDFGenerator;

public class NLPTest {

	@Test
	public void testTripleGeneration() {
		String text = "Each agency is responsible for preparing reference material or a guide for requesting records or information from that agency.";
		Set<Triple> triples = NLP.getInstance().generateTriples(text);
		for (Triple triple : triples) {
			System.out.println(triple);
		}
		assertFalse(triples.isEmpty());
		assertTrue(triples.contains(Triple.narrower("material", "reference material")));
	}
	
	@Test
	public void testSimpleVerbGeneration() {
		String text = "A rare black squirrel has become a regular visitor to a suburban garden.";
		Set<Triple> triples = NLP.getInstance().generateTriples(text);
		for (Triple triple : triples) {
			System.out.println(triple);
		}
		assertTrue(triples.contains(Triple.lii("squirrel", "visitor", "become")));
		
		text = "The quick brown fox jumps over the lazy dog.";
		triples = NLP.getInstance().generateTriples(text);
		for (Triple triple : triples) {
			System.out.println(triple);
		}
		assertTrue(triples.contains(Triple.lii("fox", "dog", "jumps")));
	}
	
	@Test
	public void testPronounResolution() {
		String text = "Caleb likes dogs. He thinks they are cute.";
		String result = NLP.getInstance().resolvePronouns(text);
		assertEquals("Caleb likes dogs. Caleb thinks they are cute.", result);
	}
	
	@Test
	public void testValidTriple() {
		Triple t = Triple.lii("states", "Florida", " (shall include) ");
		assertEquals("shall include", t.predicate);
	}
	
	@Test
	public void testRDF() throws Exception {
		RDFGenerator gen = new RDFGenerator();
		Collection<Triple> triples = new ArrayList<Triple>();
		
		Triple t = Triple.lii("people", "pizza", "eat many");
		triples.add(t);
		t = Triple.lii("people", "hamburgers", "eat many");
		triples.add(t);
		t = Triple.broader("blue dragons", "dragons");
		triples.add(t);
		
		gen.buildModel(triples);
		gen.writeTo("/tmp/test_RDF.rdf");
	}

}

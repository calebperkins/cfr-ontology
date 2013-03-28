package org.liicornell.cfr.preprocessor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.liicornell.cfr.nlp.NLP;
import org.liicornell.cfr.nlp.Triple;
import org.liicornell.cfr.nlp.TripleGenerator;
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
	public void testX() {
		String text = " As soon as practicable after the close of the hearing and after consideration of any timely objections filed as to the transcript or recording, the Judge shall issue an order making any corrections to the transcript or recording which the Judge finds are warranted, which corrections shall be entered onto the original transcript or recording by the Hearing Clerk (without obscuring the original text).";
		Set<Triple> triples = NLP.getInstance().generateTriples(text);
		for (Triple triple : triples) {
			System.out.println(triple);
		}
	}
	
	@Test
	public void testSentenceFormatting() {
		String text = " Agencies of USDA shall comply with the time limits set forth in the FOIA and in this subpart for responding to and processing requests and appeals for agency records, unless there are unusual circumstances within the meaning of <aref type=\"USC\">\n                            <subref title=\"5\" sect=\"552\" note=\"\" psec=\"#a_6_B\" tq=\"N\" target=\"http://www.law.cornell.edu/uscode/5/552.html#a_6_B\">5 U.S.C. 552(a)(6)(B)</subref>\n                        </aref> and <aref type=\"CFR-TIC-SECT\">\u00a7 <subref title=\"7\" part=\"1\" sect=\"16\" psec=\"#b\" tq=\"N\">1.16(b)</subref>\n                        </aref>. An agency shall notify a requester in writing whenever it is unable to respond to or process a request or appeal within the time limits established by the FOIA.";
		System.out.println(TripleGenerator.preprocessText(text));	
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

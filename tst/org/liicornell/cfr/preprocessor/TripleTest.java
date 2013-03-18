package org.liicornell.cfr.preprocessor;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

public class TripleTest {

	@Test
	public void test() {
		String text = "Each agency is responsible for preparing reference material or a guide for requesting records or information from that agency.";
		Set<Triple> triples = NLP.getInstance().generateTriples(text);
		for (Triple triple : triples) {
			System.out.println(triple);
		}
		assertFalse(triples.isEmpty());
	}

}

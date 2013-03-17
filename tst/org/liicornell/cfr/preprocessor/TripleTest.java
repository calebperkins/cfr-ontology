package org.liicornell.cfr.preprocessor;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

public class TripleTest {

	@Test
	public void test() {
		Set<Triple> triples = NLP.getInstance().generateTriples("Caleb likes cats and dogs. Caleb also enjoys fish.");
		for (Triple triple : triples) {
			System.out.println(triple);
		}
		fail("Not yet implemented");
	}

}

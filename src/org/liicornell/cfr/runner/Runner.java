package org.liicornell.cfr.runner;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.liicornell.cfr.nlp.Triple;
import org.liicornell.cfr.nlp.TripleGenerator;

public class Runner {

	public static void main(String[] args) throws Exception {
		// parse arguments
		if (args.length != 2) {
			System.err.println("Usage: Runner input_file output_file");
		}
		String input = args[0];
		String output = args[1];
		
		ExecutorService pool = Executors.newFixedThreadPool(7);
		RDFGenerator rdfGenerator = new RDFGenerator();
		Set<Triple> triples = new HashSet<Triple>();

		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(input);
		Element rootNode = doc.getRootElement();
		ElementFilter filter = new ElementFilter("text");
		
		// each text tag is processed separately
		for (Element c : rootNode.getDescendants(filter)) {
			pool.execute(new TripleGenerator(triples, c.getText()));
		}
		
		// wait for threads to finish and build RDF file
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		rdfGenerator.buildModel(triples);
		rdfGenerator.writeTo(output);
	}

}

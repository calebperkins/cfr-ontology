package org.liicornell.cfr.runner;

import java.io.File;
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
	
	private static void processFile(SAXBuilder builder, ElementFilter filter, File f, File out) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(7);
		Document doc = builder.build(f);
		Element rootNode = doc.getRootElement();
		
		RDFGenerator rdfGenerator = new RDFGenerator();
		Set<Triple> triples = new HashSet<Triple>();
		
		// each text tag is processed separately
		for (Element c : rootNode.getDescendants(filter)) {
			pool.execute(new TripleGenerator(triples, c.getText()));
		}
		
		// wait for threads to finish and build RDF file
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
		rdfGenerator.buildModel(triples);
		rdfGenerator.writeTo(out);
	}

	public static void main(String[] args) throws Exception {
		// parse arguments
		if (args.length != 2) {
			System.err.println("Usage: Runner input_file output_file");
		}		
		File input = new File(args[0]);
		File output = new File(args[1]);
		
		SAXBuilder builder = new SAXBuilder();
		ElementFilter filter = new ElementFilter("text");		
		
		if (input.isDirectory()) {
			for (File in : input.listFiles()) {
				System.out.println("Processing " + in);
				File out = new File(output, in.getName());
				try {
					processFile(builder, filter, in, out);
				} catch (Exception ex) {
					System.err.println("Error processing " + in.getName() + ": " + ex);
				}
			}
		} else {
			processFile(builder, filter, input, output);
		}
	}

}

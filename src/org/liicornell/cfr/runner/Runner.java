package org.liicornell.cfr.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.liicornell.cfr.corenlp.StanfordTripleGenerator;
import org.liicornell.cfr.opennlp.OpenNLPTripleGenerator;
import org.liicornell.cfr.rdf.RDFGenerator;
import org.liicornell.cfr.rdf.Triple;

/**
 * The main entry point into the Vocabulary Extraction tool.
 * 
 * @author Caleb Perkins (ctp34@cornell.edu)
 * 
 */
public class Runner {
	private final SAXBuilder builder;
	private final ElementFilter filter;
	private final boolean useStanfordParser;
	private final Map<String, String> geoNames;
	private final boolean verbose;

	public Runner(boolean stanfordParser, boolean verbose) throws IOException {
		builder = new SAXBuilder();
		filter = new ElementFilter("text");
		useStanfordParser = stanfordParser;
		geoNames = parseGeonames();
		this.verbose = verbose;
	}

	/**
	 * GeoName URIs are preprocessed and stored in a file called "geoids.txt" in the datasets directory.
	 * @return an immutable mapping of geographic entity names to their GeoName URIs
	 * @throws IOException if the file cannot be read or found
	 */
	private static Map<String, String> parseGeonames() throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		String dir = System.getProperty("cornell.datasets.dir");
		File f = new File(new File(dir, "geonames"), "geoids.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		while ((line = br.readLine()) != null) {
			String[] split = line.split("\\|");
			assert split.length == 2;
			map.put(split[1].toLowerCase(), split[0]);
		}
		br.close();
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Extract triples from an XML file into an RDF file.
	 * @param in an XML file from the CFR
	 * @param out the destination RDF file
	 * @throws Exception
	 */
	public void processFile(final File in, final File out) throws Exception {
		// TODO reuse thread pool across files
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		Document doc = builder.build(in);
		Element rootNode = doc.getRootElement();

		RDFGenerator rdfGenerator = new RDFGenerator(geoNames);

		Set<Triple> triples = new HashSet<Triple>();

		// each text tag is processed separately
		for (Element c : rootNode.getDescendants(filter)) {
			Runnable r = useStanfordParser ? new StanfordTripleGenerator(triples, c.getText())
					: new OpenNLPTripleGenerator(triples, c.getText());
			pool.execute(r);
		}

		// wait for threads to finish and build RDF file
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
		if (verbose) {
			for (Triple triple : triples) {
				System.out.println(triple);
			}
		}

		rdfGenerator.buildModel(triples);
		rdfGenerator.writeTo(out);
	}

	public static void main(String[] args) throws Exception {
		// parse arguments
		if (args.length < 2) {
			System.err.println("Usage: Runner input_file_or_directory output_file_or_directory [-useStanfordParser] [-verbose]");
			System.exit(-1);
		}
		File input = new File(args[0]);
		File output = new File(args[1]);
		boolean useStanfordParser = false;
		boolean verbose = false;
		for (String arg : args) {
			if (arg.equals("-useStanfordParser")) {
				useStanfordParser = true;
			} else if (arg.equals("-verbose")) {
				verbose = true;
			}
		}

		Runner runner = new Runner(useStanfordParser, verbose);

		if (input.isDirectory()) {
			output.mkdirs();
			for (File in : input.listFiles()) {
				System.out.println("Processing " + in);
				File out = new File(output, in.getName() + ".rdf");
				try {
					runner.processFile(in, out);
				} catch (Exception ex) {
					System.err.println("Error processing " + in.getName() + ": " + ex);
				}
			}
		} else {
			runner.processFile(input, output);
		}
	}

}

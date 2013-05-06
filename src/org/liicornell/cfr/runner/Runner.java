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
 * The command line interface into the Vocabulary Extraction tool. Once compiled as a JAR, you can run the JAR using this syntax:
 * 
 * <pre>
 * <code>java -Xms3072M -Xmx3072M -Dcornell.datasets.dir=<b>DATASETS_DIR</b> -jar VocabularyExtraction.jar <b>INPUT_DIR</b> <b>OUTPUT_DIR</b> [-useStanfordParser]</code>
 * </pre>
 * 
 * <dl>
 * <dt>DATASETS_DIR</dt>
 * <dd>replace this with an absolute path to the datasets folder</dd>
 * <dt>INPUT_DIR</dt>
 * <dd>this can be either a folder of XML files or a path to an individual XML file.</dd>
 * <dt>OUTPUT_DIR</dt>
 * <dd>the folder where you want RDF files to be output to. If you are processing a single XML file, this will be a single RDF path.</dd>
 * <dt>-useStanfordParser</dt>
 * <dd>if you include this flag, use the Stanford CoreNLP parser. It is slower and gives different results.</dd>
 * </dl>
 * 
 * As an example, here's how to process an individual file:
 * 
 * <pre><code>java -Xms3072M -Xmx3072M -Dcornell.datasets.dir=~/Downloads/VocabularyExtraction/datasets/ -jar VocabularyExtraction.jar ~/Desktop/Title7/part-1.xml ~/Desktop/Output/Title7/part-1.rdf</code></pre>
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
	
	/**
	 * The path to the datasets folder, including the trailing space.
	 */
	public static final String DATASETS_PATH = System.getProperty("cornell.datasets.dir");

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
		File f = new File(new File(DATASETS_PATH, "geonames"), "geoids.txt");
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
	 * @throws Exception if anything went wrong
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

	/**
	 * The main method. Accepts arguments as documented above.
	 * @param args an array of arguments from the command line, as documented above.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// parse arguments
		if (args.length < 2) {
			System.err.println("You must provide the arguments: input/file/or/directory output/file/or/directory [-useStanfordParser] [-verbose]");
			System.exit(-1);
		}
		if (DATASETS_PATH == null) {
			System.err.println("You must provide the JVM argument -Dcornell.datasets.dir. Refer to the README.");
			System.exit(-1);
		}
		if (System.getProperty("WNSEARCHDIR") == null) {
			System.setProperty("WNSEARCHDIR", DATASETS_PATH + "dict");
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

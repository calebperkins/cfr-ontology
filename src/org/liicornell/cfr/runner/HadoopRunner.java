package org.liicornell.cfr.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.classifier.bayes.XmlInputFormat;
import org.liicornell.cfr.opennlp.OpenNLPPipeline;

public class HadoopRunner extends Configured implements Tool {
	private static class Map extends MapReduceBase implements
			Mapper<Text, Text, Text, Text> {

		private static final XMLInputFactory factory = XMLInputFactory
				.newInstance();

		private final Set<String> agenciesToRemove = new HashSet<String>();
		private boolean resolvePronouns = false;
		private final Text outValue = new Text();

		private static String extractSentence(String document)
				throws IOException {

			try {
				XMLStreamReader reader = factory
						.createXMLStreamReader(new StringReader(document));
				String s = "";
				while (reader.hasNext()) {
					int code = reader.next();
					switch (code) {
					case XMLStreamReader.CHARACTERS:
						s += reader.getText();
					}
				}
				reader.close();
				return s;
			} catch (XMLStreamException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public void configure(JobConf job) {
			try {
				Path[] agencyFiles = DistributedCache.getLocalCacheFiles(job);
				if (job.getBoolean("cfr.remove.agencies", false)) {
					for (Path path : agencyFiles) {
						parseAgencyFile(path);
					}
				}
			} catch (IOException e) {
				System.err.println(e);
			}
			resolvePronouns = job.getBoolean("cfr.nlp.resolve.pronouns", false);
		}

		private void parseAgencyFile(Path path) throws IOException {
			BufferedReader fis = new BufferedReader(new FileReader(
					path.toString()));
			String agency = null;
			while ((agency = fis.readLine()) != null) {
				agenciesToRemove.add(agency);
			}
			fis.close();
			System.out.println("Read agencies");
		}

		@Override
		public void map(Text key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			String document = value.toString();
			String s = extractSentence(document);
			s = processSentence(s);
			outValue.set(s);
			output.collect(key, outValue);
		}

		private String processSentence(String sentence) {
			// Remove anything in <>
			sentence = sentence.replaceAll("\\<.*?\\>", "");
			// Remove and\or and/or
			sentence = sentence.replace("and/or", "or");
			sentence = sentence.replace("and\\or", "or");
			sentence = sentence.replaceAll("\n", "");
			sentence = sentence.replaceAll("( )+", " ");
			sentence = sentence.replaceAll("§", "Section");
			sentence = sentence.replaceAll(";", ".");
			sentence = sentence.replaceAll(":", ".");
			// Remove numbers
			sentence = sentence.replaceAll("\\d*(\\.)*\\d*", "");
			// Replace oxford comma
			sentence = sentence.replaceAll(", or", " or");
			sentence = sentence.replaceAll(", and", " and");
			sentence = sentence + ".";
			
			if (resolvePronouns) {
				sentence = OpenNLPPipeline.getInstance().resolvePronouns(sentence);
			}

			for (String agency : agenciesToRemove) {
				sentence = sentence.replaceAll(agency, "Agency");
			}

			return sentence;
		}

	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(null, new HadoopRunner(), args);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), HadoopRunner.class);
		conf.setJobName("preprocess");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(Map.class);

		conf.setInputFormat(XmlInputFormat.class);
		conf.set("xmlinput.start", "<text>");
		conf.set("xmlinput.end", "</text>");
		conf.setOutputFormat(TextOutputFormat.class);
		
		// Reuse the JVM to conserve memory
		conf.setInt("mapred.job.reuse.jvm.num.tasks", -1);

		ArrayList<String> other_args = new ArrayList<String>();
		for (int i = 0; i < args.length; ++i) {
			if ("-agencies".equals(args[i])) {
				conf.setBoolean("cfr.remove.agencies", true);
				DistributedCache
						.addCacheFile(new Path(args[++i]).toUri(), conf);
			} else if ("-acts".equals(args[i])) {
				conf.setBoolean("cfr.remove.acts", true);
				DistributedCache
						.addCacheFile(new Path(args[++i]).toUri(), conf);
			} else if ("-resolvePronouns".equals(args[i])) {
				conf.setBoolean("cfr.nlp.resolve.pronouns", true);
			} else {
				other_args.add(args[i]);
			}
		}

		FileInputFormat.setInputPaths(conf, new Path(other_args.get(0)));
		FileOutputFormat.setOutputPath(conf, new Path(other_args.get(1)));

		JobClient.runJob(conf);
		return 0;
	}

}

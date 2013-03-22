package org.liicornell.cfr.runner;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.liicornell.cfr.nlp.NLP;

public class Runner {

	private static class Worker implements Runnable {
		private String text;

		public Worker(String t) {
			text = t;
		}
		
		private static String processSentence(String sentence) {
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
			return sentence;
		}

		@Override
		public void run() {
			text = processSentence(text);
			String resolved = NLP.getInstance().resolvePronouns(text);
			System.out.println(resolved);
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static void main(String[] args) throws JDOMException, IOException {
		ExecutorService pool = Executors.newFixedThreadPool(4);

		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build("/tmp/part-1.xml");
		Element rootNode = doc.getRootElement();
		ElementFilter filter = new ElementFilter("text");
		for (Element c : rootNode.getDescendants(filter)) {
			pool.execute(new Worker(c.getText()));
		}
//		NLP.getInstance().resolvePronouns("Caleb likes dogs. He thinks they are cute.");
//		new Worker("").run();
	}

}

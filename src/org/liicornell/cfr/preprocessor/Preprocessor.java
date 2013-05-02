package org.liicornell.cfr.preprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Cleans up a block of XML text into a format more suitable for the NLP pipeline.
 * @author Caleb Perkins
 *
 */
public class Preprocessor {
	public static final Collection<String> agenciesToRemove = new ArrayList<String>();

	static {
		try {
			File agencies = new File(System.getProperty("cornell.datasets.dir"), "agencies.txt");
			BufferedReader fis = new BufferedReader(new FileReader(agencies));
			String agency = null;
			while ((agency = fis.readLine()) != null) {
				agenciesToRemove.add(agency);
			}
			fis.close();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Clean up the raw XML text
	 * @param text raw XML text from the CFR
	 * @return a cleaned up string that contains the sentences contained in the text
	 */
	public static String preprocessText(String text) {
		// Remove anything in <>
		text = text.replaceAll("\\<.*?\\>", "");
		// Remove and\or and/or
		text = text.replace("and/or", "or");
		text = text.replace("and\\or", "or");
		text = text.replaceAll("\n", "");
		text = text.replaceAll("( )+", " ");
		text = text.replaceAll("§|¤", "Section");
		text = text.replaceAll(";|:", ".");

		text = text.replaceAll("\\(.*\\)", ""); // Remove (X) and ()
		text = text.replaceAll("\\d+(\\.*)", ""); // Remove numbers

		text = text.replaceAll("Ò|Ó", "\"");

		// Replace oxford comma
		text = text.replaceAll(", or", " or");
		text = text.replaceAll(", and", " and");

		for (String agency : agenciesToRemove) {
			text = text.replaceAll(agency, "Agency");
		}

		return text;
	}
}

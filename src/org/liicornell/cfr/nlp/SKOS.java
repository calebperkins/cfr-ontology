package org.liicornell.cfr.nlp;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class SKOS {

	public static String URI = "http://www.w3.org/2004/02/skos/core#";
	public static String LII_URI = "http://liicornell.org/liivoc/";

	protected static final Property property(String local) {
		return ResourceFactory.createProperty(URI, local);
	}

	public static Property broader = property("broader");
	public static Property narrower = property("narrower");
	public static Property related = property("related");

	public static Property prefLabel = property("prefLabel");

	public static String enLang = "xml:lang='en'";

}

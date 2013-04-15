package org.liicornell.cfr.rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class RDFGenerator {
	public static String LII_URI = "http://liicornell.org/liivoc#";

	private final Model model;

	public RDFGenerator() {
		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("liivoc", LII_URI);
		model.setNsPrefix("skos", SKOS.URI);
	}

	public void buildModel(Collection<Triple> triples) {
		for (Triple t : triples) {
			if (t.subject.length() <= 2 || t.predicate.length() <= 2 || t.object.length() <= 2) {
				continue;
			}
			add(t);
		}
	}

	public void writeTo(String fileName) throws IOException {
		model.write(new FileOutputStream(fileName));
	}
	
	public void writeTo(File file) throws IOException {
		model.write(new FileOutputStream(file));
	}

	private void add(Triple t) {
		Resource sub = liiResource(t.subject);
		if (sub == null) return;
		Resource obj = liiResource(t.object);
		if (obj == null) return;

		obj.addProperty(SKOS.prefLabel, t.object);
		obj.addProperty(RDFS.label, t.object);

		sub.addProperty(SKOS.prefLabel, sub);
		sub.addProperty(RDFS.label, sub);
		sub.addProperty(getProperty(t.predicate), obj);
	}

	private Property getProperty(String predicate) {
		if (predicate.equals(Triple.BROADER))
			return SKOS.broader;
		if (predicate.equals(Triple.NARROWER))
			return SKOS.narrower;
		if (predicate.equals(Triple.RELATED))
			return SKOS.related;
		createPredicateDescription(predicate);
		return model.createProperty(LII_URI, toURI(predicate));
	}

	private void createPredicateDescription(String predicate) {
		Resource pred = liiResource(predicate);
		pred.addProperty(RDFS.label, predicate);
		pred.addProperty(RDF.type, OWL.ObjectProperty);
		pred.addProperty(RDF.type, RDF.Property);
	}

	private Resource liiResource(String s) {
		String uri = toURI(s);
		if (uri.isEmpty())
			return null;
		return model.createResource(LII_URI + toURI(s));
	}

	/**
	 * Make a valid URI from a string. Use this when you want to preserve
	 * certain information, like spaces, but have a valid resource URI.
	 * 
	 * @param s
	 *            a string with most garbage already removed
	 * @return a hopefully valid URI
	 */
	private static String toURI(String s) {
		return s.replace(" ", "_").replaceAll("%|\\$|#|\\]|\\[", "");
	}

	@Override
	public String toString() {
		return model.toString();
	}
}

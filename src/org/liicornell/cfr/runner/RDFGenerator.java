package org.liicornell.cfr.runner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.liicornell.cfr.nlp.SKOS;
import org.liicornell.cfr.nlp.Triple;

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
			if (t.subject.contains("%") || t.object.contains("%") || t.predicate.contains("%")) {
				continue;
			}
			add(t);
		}
	}
	
	public void writeTo(String fileName) throws IOException {
		model.write(new FileOutputStream(fileName));
	}

	private void add(Triple t) {
		Resource sub = liiResource(t.subject);
		Resource obj = liiResource(t.object);

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
		return model.createProperty(LII_URI, predicate.replace(" ", "_"));
	}

	private void createPredicateDescription(String predicate) {
		Resource pred = liiResource(predicate);
		pred.addProperty(RDFS.label, predicate);
		pred.addProperty(RDF.type, OWL.ObjectProperty);
		pred.addProperty(RDF.type, RDF.Property);
	}

	private Resource liiResource(String s) {
		s = s.replace(" ", "_");
		return model.createResource(LII_URI + s);
	}
	
	@Override
	public String toString() {
		return model.toString();
	}
}

package org.liicornell.cfr.rdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A wrapper around Jena to generate RDF files from Triple objects.
 * @author Caleb Perkins
 *
 */
public class RDFGenerator {

	private final Model model;
	
	// Mapping of city, country names to URIs
	private final Map<String, String> geonames;
	
	public RDFGenerator() {
		this(new HashMap<String, String>());
	}
	
	public RDFGenerator(Map<String, String> geonames) {
		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("liivoc", LII.URI);
		model.setNsPrefix("skos", SKOS.URI);
		model.setNsPrefix("geo", GeoNames.URI);
		this.geonames = geonames;
	}

	/**
	 * Add all the triples to this model
	 * @param triples the collection of triples
	 */
	public void buildModel(Iterable<Triple> triples) {
		for (Triple t : triples) {
			if (t.subject.length() <= 2 || t.predicate.length() <= 2 || t.object.length() <= 2) {
				continue;
			}
			add(t);
		}
	}
	
	public void writeTo(File file) throws IOException {
		model.write(new FileOutputStream(file));
	}

	private void add(Triple t) {
		Resource sub = makeResource(t.subject);
		if (sub == null) return;
		Resource obj = makeResource(t.object);
		if (obj == null) return;

		obj.addProperty(SKOS.prefLabel, t.object);
		obj.addProperty(RDFS.label, t.object);

		sub.addProperty(SKOS.prefLabel, sub);
		sub.addProperty(RDFS.label, sub);
		sub.addProperty(makeProperty(t.predicate), obj);
	}

	private Property makeProperty(String predicate) {
		if (predicate.equals(Triple.BROADER))
			return SKOS.broader;
		if (predicate.equals(Triple.NARROWER))
			return SKOS.narrower;
		if (predicate.equals(Triple.RELATED))
			return SKOS.related;
		makePredicateDescription(predicate);
		return model.createProperty(LII.URI, toURI(predicate));
	}

	private void makePredicateDescription(String predicate) {
		Resource pred = makeResource(predicate);
		pred.addProperty(RDFS.label, predicate);
		pred.addProperty(RDF.type, OWL.ObjectProperty);
		pred.addProperty(RDF.type, RDF.Property);
	}

	/**
	 * Create either a GeoNames or custom LII resource from an entity.
	 * @param s the resource name
	 * @return a GeoNames or LII resource with the given name
	 */
	private Resource makeResource(String s) {
		String uri = geonames.get(s);
		if (uri != null)
			return model.createResource(uri);
		uri = toURI(s);
		if (uri.isEmpty())
			return null;
		return model.createResource(LII.URI + uri);
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

package org.liicornell.cfr.rdf;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class GeoNames {
	public static String URI = "http://www.geonames.org/ontology#";
	
	public static final Property property(String local) {
		return ResourceFactory.createProperty(URI, local);
	}
	
	public static final Resource resource(String r) {
		return ResourceFactory.createProperty(URI, r);
	}
}

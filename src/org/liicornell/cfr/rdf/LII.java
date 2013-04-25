package org.liicornell.cfr.rdf;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class LII {
	public static String URI = "http://liicornell.org/liivoc#";
	
	public static final Property property(String concept) {
		return ResourceFactory.createProperty(URI, concept);
	}
	
	public static final Resource resource(String concept) {
		return ResourceFactory.createResource(URI + concept);
	}
}

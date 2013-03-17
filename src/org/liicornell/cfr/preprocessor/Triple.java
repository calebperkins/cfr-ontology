package org.liicornell.cfr.preprocessor;

public class Triple {
	public final String subject;
	public final String object;
	public final Predicate predicate;
	
	public static enum Predicate {
		BROADER,
		NARROWER,
		RELATED
	}
	
	private Triple(String subject, String object, Predicate predicate) {
		this.subject = subject.trim();
		this.object = object.trim();
		this.predicate = predicate;
	}
	
	@Override
	public String toString() {
		return String.format("(%s, %s, %s)", subject, object, predicate);
	}
	
	public static Triple narrower(String subject, String object) {
		return new Triple(subject, object, Predicate.NARROWER);
	}
	
	public static Triple broader(String subject, String object) {
		return new Triple(subject, object, Predicate.BROADER);
	}
	
	public static Triple related(String subject, String object) {
		return new Triple(subject, object, Predicate.RELATED);
	}
}

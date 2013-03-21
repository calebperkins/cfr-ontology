package org.liicornell.cfr.nlp;

public class Triple {
	public final String subject;
	public final String object;
	public final String predicate;
	
	private static final String BROADER = "skos:broader";
	private static final String NARROWER = "skos:narrower";
	private static final String RELATED = "skos:related";
	
	private Triple(String subject, String object, String predicate) {
		this.subject = subject.trim();
		this.object = object.trim();
		this.predicate = predicate;
	}
	
	@Override
	public String toString() {
		return String.format("(%s, %s, %s)", subject, object, predicate);
	}
	
	public static Triple narrower(String subject, String object) {
		return new Triple(subject, object, NARROWER);
	}
	
	public static Triple broader(String subject, String object) {
		return new Triple(subject, object, BROADER);
	}
	
	public static Triple related(String subject, String object) {
		return new Triple(subject, object, RELATED);
	}
	
	public static Triple lii(String subject, String object, String predicate) {
		return new Triple(subject, object, "liivoc:" + predicate);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Triple other = (Triple) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}
}

package org.liicornell.cfr.rdf;

/**
 * Represents a triple object, with a given subject, object, and predicate.
 * 
 * This class is meant to be a simple data container. It does minimal logic to
 * format the given strings, such as trimming them and replacing invalid
 * characters.
 * 
 * @author Caleb Perkins (ctp34@cornell.edu)
 * 
 */
public class Triple {
	public final String subject;
	public final String object;
	public final String predicate;

	public static final String BROADER = "skos:broader";
	public static final String NARROWER = "skos:narrower";
	public static final String RELATED = "skos:related";

	private Triple(String subject, String object, String predicate) {
		this.subject = format(subject);
		this.object = format(object);
		this.predicate = format(predicate);
	}

	public static String format(String s) {
		return s.replace('Ñ', '_').replaceAll(",|\\(|\\)", "").toLowerCase().trim();
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
		return new Triple(subject, object, predicate);
	}

	/**
	 * Inverts a triple, turning a skos:narrower into a skos:broader.
	 * 
	 * @return a new inverted triple
	 */
	public Triple inversion() {
		if (predicate.equals(BROADER))
			return new Triple(object, subject, NARROWER);
		if (predicate.equals(NARROWER))
			return new Triple(object, subject, BROADER);
		return new Triple(object, subject, RELATED);
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

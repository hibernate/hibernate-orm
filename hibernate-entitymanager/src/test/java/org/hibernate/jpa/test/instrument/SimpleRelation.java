package org.hibernate.jpa.test.instrument;

/**
 * A simple entity relation used by {@link Simple} to ensure that enhanced
 * classes load all classes.
 * 
 * @author Dustin Schultz
 */
public class SimpleRelation {

	private String blah;

	public String getBlah() {
		return blah;
	}

	public void setBlah(String blah) {
		this.blah = blah;
	}

}

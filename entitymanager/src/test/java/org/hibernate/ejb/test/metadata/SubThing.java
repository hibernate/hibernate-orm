package org.hibernate.ejb.test.metadata;

/**
 * @author Emmanuel Bernard
 */
//not an entity but in between mapped superclass and entity
public class SubThing extends Thing {
	private String blah;

	public String getBlah() {
		return blah;
	}

	public void setBlah(String blah) {
		this.blah = blah;
	}
}

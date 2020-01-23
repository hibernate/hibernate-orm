package org.hibernate.envers.test.embeddedid;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class CompositeEntityId implements Serializable{

	private String firstCode;
	private String secondCode;

	public String getFirstCode() {
		return firstCode;
	}

	public void setFirstCode(String firstCode) {
		this.firstCode = firstCode;
	}

	public String getSecondCode() {
		return secondCode;
	}

	public void setSecondCode(String secondCode) {
		this.secondCode = secondCode;
	}
}

package org.hibernate.test.formulajoin;

import java.io.Serializable;


public class OtherEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	private Id id;
	
	public OtherEntity() {
		super();
	}
	
	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}
}

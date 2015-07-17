package org.hibernate.test.formulajoin;

import java.io.Serializable;

public class Entity implements Serializable {
	private static final long serialVersionUID = 1L;

	private Id id;

	private OtherEntity other;

	public Entity() {
		super();
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public OtherEntity getOther() {
		return other;
	}

	public void setOther(OtherEntity partial) {
		this.other = partial;
	}
}

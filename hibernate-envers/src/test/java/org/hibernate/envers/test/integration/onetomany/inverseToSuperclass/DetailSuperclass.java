package org.hibernate.envers.test.integration.onetomany.inverseToSuperclass;

import org.hibernate.envers.Audited;

@Audited
public class DetailSuperclass {

	private long id;

	private Master parent;

	public DetailSuperclass() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Master getParent() {
		return parent;
	}

	public void setParent(Master parent) {
		this.parent = parent;
	}

}

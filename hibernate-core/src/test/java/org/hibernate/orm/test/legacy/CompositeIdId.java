/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.io.Serializable;

/**
 * @author max
 *
 */
public class CompositeIdId implements Serializable {

	String user;
	String id;
	String name;
	CompositeElement composite;


	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		// not totally NP or type safe equals, but enough for the unittests.
		CompositeIdId o = (CompositeIdId) obj;
		if(o==null) return false;
		return o.getUser().equals( getUser()) && o.getId().equals(getId());
	}
	public CompositeElement getComposite() {
		return composite;
	}
	public void setComposite(CompositeElement composite) {
		this.composite = composite;
	}
}

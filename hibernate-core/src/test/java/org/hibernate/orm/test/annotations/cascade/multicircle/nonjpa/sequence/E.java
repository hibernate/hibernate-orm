/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.sequence;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class E extends AbstractEntity {
	private static final long serialVersionUID = 1226955558L;

	@jakarta.persistence.OneToMany(mappedBy = "e")
	private java.util.Set<D> dCollection = new java.util.HashSet<D>();

	@jakarta.persistence.ManyToOne(optional = true)
	private F f;

	public java.util.Set<D> getDCollection() {
		return dCollection;
	}
	public void setDCollection(java.util.Set<D> dCollection) {
		this.dCollection = dCollection;
	}

	public F getF() {
		return f;
	}
	public void setF(F parameter) {
		this.f = parameter;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.sequence;

import java.util.Set;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class C extends AbstractEntity {
	private static final long serialVersionUID = 1226955752L;

	@jakarta.persistence.OneToMany(mappedBy = "c")
	private Set<B> bCollection = new java.util.HashSet<B>();

	@jakarta.persistence.OneToMany(mappedBy = "c")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})

	private Set<D> dCollection = new java.util.HashSet<D>();

	public Set<B> getBCollection() {
		return bCollection;
	}

	public void setBCollection(Set<B> bCollection) {
		this.bCollection = bCollection;
	}

	public Set<D> getDCollection() {
		return dCollection;
	}

	public void setDCollection(Set<D> dCollection) {
		this.dCollection = dCollection;
	}

}

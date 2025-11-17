/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.identity;

import java.util.Set;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class EntityC extends AbstractEntity {
	private static final long serialVersionUID = 1226955752L;

	@jakarta.persistence.OneToMany(mappedBy = "c")
	private Set<EntityB> bCollection = new java.util.HashSet<EntityB>();

	@jakarta.persistence.OneToMany(mappedBy = "c")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})

	private Set<EntityD> dCollection = new java.util.HashSet<EntityD>();

	public Set<EntityB> getBCollection() {
		return bCollection;
	}

	public void setBCollection(Set<EntityB> bCollection) {
		this.bCollection = bCollection;
	}

	public Set<EntityD> getDCollection() {
		return dCollection;
	}

	public void setDCollection(Set<EntityD> dCollection) {
		this.dCollection = dCollection;
	}

}

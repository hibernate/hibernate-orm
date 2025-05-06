/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.identity;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class EntityE extends AbstractEntity {
	private static final long serialVersionUID = 1226955558L;

	@jakarta.persistence.OneToMany(mappedBy = "e")
	private java.util.Set<EntityD> dCollection = new java.util.HashSet<EntityD>();

	@jakarta.persistence.ManyToOne(optional = true)
	private EntityF f;

	public java.util.Set<EntityD> getDCollection() {
		return dCollection;
	}
	public void setDCollection(java.util.Set<EntityD> dCollection) {
		this.dCollection = dCollection;
	}

	public EntityF getF() {
		return f;
	}
	public void setF(EntityF parameter) {
		this.f = parameter;
	}
}

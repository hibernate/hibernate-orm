/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.identity;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class EntityG extends AbstractEntity {
	private static final long serialVersionUID = 325417437L;

	@jakarta.persistence.ManyToOne(optional = false)
	private EntityB b;

	@jakarta.persistence.OneToMany(mappedBy = "g")
	private java.util.Set<EntityF> fCollection = new java.util.HashSet<EntityF>();

	public EntityB getB() {
		return b;
	}
	public void setB(EntityB parameter){
		this.b = parameter;
	}

	public java.util.Set<EntityF> getFCollection() {
		return fCollection;
	}
	public void setFCollection(
		java.util.Set<EntityF> parameter) {
		this.fCollection = parameter;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.identity;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class EntityD extends AbstractEntity {
	private static final long serialVersionUID = 2417176961L;

	@jakarta.persistence.OneToMany(mappedBy = "d")
	private java.util.Set<EntityB> bCollection = new java.util.HashSet<EntityB>();

	@jakarta.persistence.ManyToOne(optional = false)
	private EntityC c;

	@jakarta.persistence.ManyToOne(optional = false)
	private EntityE e;

	@jakarta.persistence.OneToMany(mappedBy = "d")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private java.util.Set<EntityF> fCollection = new java.util.HashSet<EntityF>();

	public java.util.Set<EntityB> getBCollection() {
		return bCollection;
	}
	public void setBCollection(
			java.util.Set<EntityB> parameter) {
		this.bCollection = parameter;
	}

	public EntityC getC() {
		return c;
	}
	public void setC(EntityC c) {
		this.c = c;
	}

	public EntityE getE() {
		return e;
	}
	public void setE(EntityE e) {
		this.e = e;
	}

	public java.util.Set<EntityF> getFCollection() {
		return fCollection;
	}
	public void setFCollection(
			java.util.Set<EntityF> parameter) {
		this.fCollection = parameter;
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.sequence;

@jakarta.persistence.Entity
public class B extends AbstractEntity {
	private static final long serialVersionUID = 325417243L;

	@jakarta.persistence.OneToMany(mappedBy = "b")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private java.util.Set<G> gCollection = new java.util.HashSet<G>();


	@jakarta.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private C c;

	@jakarta.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private D d;

	public java.util.Set<G> getGCollection() {
		return gCollection;
	}

	public void setGCollection(
		java.util.Set<G> parameter) {
		this.gCollection = parameter;
	}

	public C getC() {
		return c;
	}

	public void setC(C parameter) {
		this.c = parameter;
	}

	public D getD() {
		return d;
	}

	public void setD(D parameter) {
		this.d = parameter;
	}

}

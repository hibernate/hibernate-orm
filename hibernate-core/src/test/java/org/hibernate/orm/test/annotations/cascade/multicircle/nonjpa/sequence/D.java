/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.sequence;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class D extends AbstractEntity {
	private static final long serialVersionUID = 2417176961L;

	@jakarta.persistence.OneToMany(mappedBy = "d")
	private java.util.Set<B> bCollection = new java.util.HashSet<B>();

	@jakarta.persistence.ManyToOne(optional = false)
	private C c;

	@jakarta.persistence.ManyToOne(optional = false)
	private E e;

	@jakarta.persistence.OneToMany(mappedBy = "d")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private java.util.Set<F> fCollection = new java.util.HashSet<F>();

	public java.util.Set<B> getBCollection() {
		return bCollection;
	}
	public void setBCollection(
			java.util.Set<B> parameter) {
		this.bCollection = parameter;
	}

	public C getC() {
		return c;
	}
	public void setC(C c) {
		this.c = c;
	}

	public E getE() {
		return e;
	}
	public void setE(E e) {
		this.e = e;
	}

	public java.util.Set<F> getFCollection() {
		return fCollection;
	}
	public void setFCollection(
			java.util.Set<F> parameter) {
		this.fCollection = parameter;
	}

}

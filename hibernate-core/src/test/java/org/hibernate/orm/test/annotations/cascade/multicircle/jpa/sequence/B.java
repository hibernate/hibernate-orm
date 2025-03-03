/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.jpa.sequence;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class B extends AbstractEntity {
	private static final long serialVersionUID = 325417243L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "b")
	private java.util.Set<G> gCollection = new java.util.HashSet<G>();


	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, optional = false)
	private C c;



	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, optional = false)
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

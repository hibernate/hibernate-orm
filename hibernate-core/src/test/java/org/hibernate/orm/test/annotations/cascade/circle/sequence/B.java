/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.sequence;

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
	private java.util.Set<C> cCollection = new java.util.HashSet<C>();

	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, optional = false)
	private A a;

	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, optional = false)
	private F f;

	public java.util.Set<C> getCCollection() {
		return cCollection;
	}

	public void setCCollection(
		java.util.Set<C> parameter) {
		this.cCollection = parameter;
	}

	public A getA() {
		return a;
	}

	public void setA(A parameter) {
		this.a = parameter;
	}

	public F getF() {
		return f;
	}

	public void setF(F parameter) {
		this.f = parameter;
	}
}

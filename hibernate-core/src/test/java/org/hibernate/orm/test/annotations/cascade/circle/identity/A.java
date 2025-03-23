/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.identity;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class A extends AbstractEntity {
	private static final long serialVersionUID = 864804063L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "a")
	private java.util.Set<B> bCollection = new java.util.HashSet<B>();

	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "aCollection")
	private java.util.Set<D> dCollection = new java.util.HashSet<D>();

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "a")
	private java.util.Set<C> cCollection = new java.util.HashSet<C>();

	public java.util.Set<B> getBCollection() {
		return bCollection;
	}

	public void setBCollection(
		java.util.Set<B> parameter) {
		this.bCollection = parameter;
	}

	public java.util.Set<D> getDCollection() {
		return dCollection;
	}

	public void setDCollection(
		java.util.Set<D> parameter) {
		this.dCollection = parameter;
	}

	public java.util.Set<C> getCCollection() {
		return cCollection;
	}

	public void setCCollection(
		java.util.Set<C> parameter) {
		this.cCollection = parameter;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.sequence;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class G extends AbstractEntity {
	private static final long serialVersionUID = 325417437L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "g")
	private java.util.Set<C> cCollection = new java.util.HashSet<C>();

	public java.util.Set<C> getCCollection() {
		return cCollection;
	}

	public void setCCollection(
		java.util.Set<C> parameter) {
		this.cCollection = parameter;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.sequence;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class C extends AbstractEntity {
	private static final long serialVersionUID = 1226955752L;

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
	)
	private G g;

	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, optional = false)
	private B b;

	public A getA() {
		return a;
	}

	public void setA(A parameter) {
		this.a = parameter;
	}

	public G getG() {
		return g;
	}

	public void setG(G parameter) {
		this.g = parameter;
	}

	public B getB() {
		return b;
	}

	public void setB(B parameter) {
		this.b = parameter;
	}
}

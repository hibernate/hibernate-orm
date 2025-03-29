/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.merge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Parent {

	@Id
	protected Long id;

	private String name;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Child child;

	public Parent() {
	}

	public Parent(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public Child getChild() {
		return this.child;
	}

	public void setChild(Child child) {
		this.child = child;
	}
}

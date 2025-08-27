/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.immutable;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;

/**
 * @author Alex Burgel
 */
@Entity
public class Child {
	@Id
	private Integer id;
	@NaturalId
	private String name;
	@NaturalId( )
	@ManyToOne
	private Parent parent;

	Child() {}

	public Child(Integer id, String name, Parent parent) {
		this.id = id;
		this.name = name;
		this.parent = parent;
	}

	public Integer getId() {
		return id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

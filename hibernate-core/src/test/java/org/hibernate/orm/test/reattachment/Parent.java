/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.reattachment;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Parent entity
 *
 * @author Steve Ebersole
 */
@Entity
public class Parent {
	@Id
	@Column(name = "NAME")
	private String name;
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
	private Set<Child> children = new HashSet<>();

	public Parent() {
	}

	public Parent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}

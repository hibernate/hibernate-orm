/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.set;
import java.util.HashSet;
import java.util.Set;

/**
 * todo: describe Parent
 *
 * @author Steve Ebersole
 */
public class Parent {
	private String name;
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

	public Set<Child> getChildren() {
		return children;
	}

	public void setChildren(Set<Child> children) {
		this.children = children;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.parent;

import java.util.HashSet;
import java.util.Set;

public class ParentEntity {
	private Long id;
	private String name;
	private Set<CompositeChild> children = new HashSet<>();
	private ComponentChild component;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<CompositeChild> getChildren() {
		return children;
	}

	public void setChildren(Set<CompositeChild> children) {
		this.children = children;
	}

	public ComponentChild getComponent() {
		return component;
	}

	public void setComponent(ComponentChild component) {
		this.component = component;
	}
}

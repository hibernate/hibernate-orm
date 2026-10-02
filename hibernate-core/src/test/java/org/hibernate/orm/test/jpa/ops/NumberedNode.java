/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;


import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class NumberedNode {

	private long id;

	private String name;
	private NumberedNode parent;
	private Set<NumberedNode> children = new HashSet<>();
	private String description;

	public NumberedNode() {
		super();
	}

	public NumberedNode(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<NumberedNode> getChildren() {
		return children;
	}

	public void setChildren(Set<NumberedNode> children) {
		this.children = children;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NumberedNode getParent() {
		return parent;
	}

	public void setParent(NumberedNode parent) {
		this.parent = parent;
	}

	public NumberedNode addChild(NumberedNode child) {
		children.add( child );
		child.setParent( this );
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

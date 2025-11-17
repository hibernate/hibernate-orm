/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;
import java.util.HashSet;
import java.util.Set;

/**
 * VersionedNode
 *
 * @author Gail Badner
 */
public class VersionedNode {
	private String id;
	private String name;
	private long version;

	private VersionedNode parent;
	private Set children = new HashSet();

	public VersionedNode() {
	}

	public VersionedNode(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public VersionedNode getParent() {
		return parent;
	}

	public void setParent(VersionedNode parent) {
		this.parent = parent;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}

	public void addChild(VersionedNode child) {
		child.setParent( this );
		children.add( child );
	}
}

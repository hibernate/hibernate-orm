/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.OptimisticLock;

/**
 * VersionedNode
 *
 * @author Gail Badner
 */
@Entity
@Table(name = "V_NODE")
public class VersionedNode {
	@Id
	@Column(name = "ID")
	private String id;
	@Column(name = "NAME")
	private String name;
	@Version
	@Column(name = "VERS")
	private long version;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REMOVE})
	private VersionedNode parent;
	@OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REMOVE})
	@OptimisticLock(excluded = false)
	private Set<VersionedNode> children = new HashSet<>();

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

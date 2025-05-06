/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 *
 * @author Chris Cranford
 */
@Entity
@Access(AccessType.PROPERTY)
public class Root {
	private Long id;
	private String name;
	private Set<Leaf> leaves = new HashSet<>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NAME", length = 50)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	public Set<Leaf> getLeaves() {
		return leaves;
	}

	public void setLeaves(Set<Leaf> leaves) {
		this.leaves = leaves;
	}
}

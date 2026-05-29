/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.identity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "my_entity")
public class MyEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private MySibling sibling;
	@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinColumn(name = "non_inv_parent_id")
	private Set<MyChild> nonInverseChildren = new HashSet<>();
	@OneToMany(mappedBy = "inverseParent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private Set<MyChild> inverseChildren = new HashSet<>();

	public MyEntity() {
	}

	public MyEntity(String name) {
		this.name = name;
	}

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

	public MySibling getSibling() {
		return sibling;
	}

	public void setSibling(MySibling sibling) {
		this.sibling = sibling;
	}

	public Set<MyChild> getNonInverseChildren() {
		return nonInverseChildren;
	}

	public void setNonInverseChildren(Set<MyChild> nonInverseChildren) {
		this.nonInverseChildren = nonInverseChildren;
	}

	public Set<MyChild> getInverseChildren() {
		return inverseChildren;
	}

	public void setInverseChildren(Set<MyChild> inverseChildren) {
		this.inverseChildren = inverseChildren;
	}
}

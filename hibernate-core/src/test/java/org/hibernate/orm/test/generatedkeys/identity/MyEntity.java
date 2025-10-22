/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.identity;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class MyEntity {
	private Long id;
	private String name;
	private MySibling sibling;
	private Set<MyChild> nonInverseChildren = new HashSet<>();
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

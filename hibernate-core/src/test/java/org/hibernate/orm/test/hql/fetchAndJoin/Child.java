/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.fetchAndJoin;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "child")
public class Child {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn
	private Set<GrandChild> grandChildren = new HashSet<GrandChild>();

	public Child() {
	}

	public Child(String value) {
		this.value = value;
	}

	@Column(name = "val")
	private String value;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<GrandChild> getGrandChildren() {
		return grandChildren;
	}

	public void setGrandChildren(Set<GrandChild> grandChildren) {
		this.grandChildren = grandChildren;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}

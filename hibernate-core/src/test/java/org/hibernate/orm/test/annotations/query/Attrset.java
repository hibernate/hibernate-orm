/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

@Entity
public class Attrset {
	@Id
	@GeneratedValue
	private Long id;

	@OneToMany
	@JoinTable(name = "ATTRSET_X_ATTRVALUE")
	private Set<Attrvalue> attrvalues = new HashSet<Attrvalue>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Attrvalue> getAttrvalues() {
		return attrvalues;
	}

	public void setAttrvalues(Set<Attrvalue> attrvalues) {
		this.attrvalues = attrvalues;
	}
}

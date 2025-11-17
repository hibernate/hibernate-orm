/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deleteunloaded;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Parent {
	@GeneratedValue
	@Id
	private long id;
	@Version
	private int version;

	@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
	private Set<Child> children = new HashSet<>();

	@ElementCollection
	private List<String> words = new ArrayList<>();

	public Set<Child> getChildren() {
		return children;
	}

	public List<String> getWords() {
		return words;
	}

	public long getId() {
		return id;
	}
}

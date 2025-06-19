/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Definition {
	private Integer id;
	private String name;
	private Set<Value> values = new HashSet<>();

	public Definition() {
	}

	public Definition(Integer id, String name) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Value> getValues() {
		return values;
	}

	public void setValues(Set<Value> values) {
		this.values = values;
	}
}

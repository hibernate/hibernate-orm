/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class Item {
	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	@ElementCollection
	@CollectionTable(name = "item_roles")
	@Convert(converter = ItemAttributeConverter.class)
	public List<Attribute> roles = new ArrayList<>();

	Item() {

	}

	Item(String name) {
		this.name = name;
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

	public List<Attribute> getRoles() {
		return roles;
	}

	public void setRoles(List<Attribute> roles) {
		this.roles = roles;
	}
}

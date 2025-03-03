/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	@ElementCollection
	@Convert(converter = ItemRoleConverter.class)
	private Set<Role> roles = new HashSet<>();

	@ElementCollection
	@Convert(converter = StringToUppercaseConverter.class)
	private List<String> providers = new ArrayList<>();

	@ElementCollection
	@Convert(converter = StringToUppercaseConverter.class)
	private Map<Integer, String> attributes = new HashMap<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public List<String> getProviders() {
		return providers;
	}

	public void setProviders(List<String> providers) {
		this.providers = providers;
	}

	public Map<Integer, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<Integer, String> attributes) {
		this.attributes = attributes;
	}
}

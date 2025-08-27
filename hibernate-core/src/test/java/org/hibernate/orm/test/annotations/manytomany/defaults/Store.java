/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany.defaults;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Store {
	private Integer id;
	private String name;
	private Set<KnownClient> customers;
	private Set<Item> items;
	private Set<Category> categories;

	@ManyToMany(cascade = CascadeType.PERSIST)
	public Set<City> getImplantedIn() {
		return implantedIn;
	}

	public void setImplantedIn(Set<City> implantedIn) {
		this.implantedIn = implantedIn;
	}

	private Set<City> implantedIn;

	@Id
	@GeneratedValue
	@Column(name="sId")
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

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	public Set<KnownClient> getCustomers() {
		return customers;
	}

	public void setCustomers(Set<KnownClient> customers) {
		this.customers = customers;
	}

	@ManyToMany
	public Set<Item> getItems() {
		return items;
	}

	public void setItems(Set<Item> items) {
		this.items = items;
	}

	@ManyToMany
	public Set<Category> getCategories() {
		return categories;
	}

	public void setCategories(Set<Category> categories) {
		this.categories = categories;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import java.util.HashSet;
import java.util.Set;

public class Product {
	private int id;

	private Set<Category> categoriesOneToMany = new HashSet<>();

	private Set<Category> categoriesWithDescOneToMany = new HashSet<>();

	private Set<Category> categoriesManyToMany = new HashSet<>();

	private Set<Category> categoriesWithDescManyToMany = new HashSet<>();

	private Set<Category> categoriesWithDescIdLt4ManyToMany = new HashSet<>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Category> getCategoriesOneToMany() {
		return categoriesOneToMany;
	}

	public void setCategoriesOneToMany(Set<Category> categoriesOneToMany) {
		this.categoriesOneToMany = categoriesOneToMany;
	}

	public Set<Category> getCategoriesWithDescOneToMany() {
		return categoriesWithDescOneToMany;
	}

	public void setCategoriesWithDescOneToMany(Set<Category> categoriesWithDescOneToMany) {
		this.categoriesWithDescOneToMany = categoriesWithDescOneToMany;
	}

	public Set<Category> getCategoriesManyToMany() {
		return categoriesManyToMany;
	}

	public void setCategoriesManyToMany(Set<Category> categoriesManyToMany) {
		this.categoriesManyToMany = categoriesManyToMany;
	}

	public Set<Category> getCategoriesWithDescManyToMany() {
		return categoriesWithDescManyToMany;
	}

	public void setCategoriesWithDescManyToMany(Set<Category> categoriesWithDescManyToMany) {
		this.categoriesWithDescManyToMany = categoriesWithDescManyToMany;
	}

	public Set<Category> getCategoriesWithDescIdLt4ManyToMany() {
		return categoriesWithDescIdLt4ManyToMany;
	}

	public void setCategoriesWithDescIdLt4ManyToMany(Set<Category> categoriesWithDescIdLt4ManyToMany) {
		this.categoriesWithDescIdLt4ManyToMany = categoriesWithDescIdLt4ManyToMany;
	}
}

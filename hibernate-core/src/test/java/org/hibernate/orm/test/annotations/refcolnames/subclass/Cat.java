/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.subclass;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * @author Marco Belladelli
 */
@Entity(name = "Cat")
public class Cat extends Animal {
	@ManyToMany
	@JoinTable(
			name = "cat_toys",
			joinColumns = @JoinColumn(name = "cat_name", referencedColumnName = "name")
	)
	private List<Toy> toys = new ArrayList<>();

	public List<Toy> getToys() {
		return toys;
	}

	public void setToys(List<Toy> toys) {
		this.toys = toys;
	}
}

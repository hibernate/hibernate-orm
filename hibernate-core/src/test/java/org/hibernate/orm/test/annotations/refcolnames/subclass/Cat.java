/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

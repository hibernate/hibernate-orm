/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Chris Cranford
 */
@Entity
public class Book {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany
	private Set<Store> stores;

	public Book() {

	}

	public Book(Set<Store> stores) {
		this.stores = stores;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Store> getStores() {
		return stores;
	}

	public void setStores(Set<Store> stores) {
		this.stores = stores;
	}
}

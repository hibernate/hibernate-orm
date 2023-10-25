/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria.one_to_one;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Janario Oliveira
 */
@Entity
public class User {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	@OneToOne(mappedBy = "user")
	private Customer customer;

	protected User() {
	}

	public User(String name) {
		this.name = name;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	@Override
	public boolean equals(Object o) {
		if ( !( o instanceof User ) ) {
			return false;
		}

		User customer = (User) o;
		return name.equals( customer.name );

	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
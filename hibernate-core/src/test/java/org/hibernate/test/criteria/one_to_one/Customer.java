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
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * @author Janario Oliveira
 */
@Entity
public class Customer {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	@OneToOne
	@JoinColumn(name = "user_id")
	private User user;

	protected Customer() {
	}

	public Customer(String name) {
		this.name = name;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public boolean equals(Object o) {
		if ( !( o instanceof Customer ) ) {
			return false;
		}

		Customer seller = (Customer) o;
		return name.equals( seller.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
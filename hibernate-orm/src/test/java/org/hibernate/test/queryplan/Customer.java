/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryplan;


/**
 * Leaf subclass
 *
 * @author Steve Ebersole
 */
public class Customer extends User {
	private String company;

	protected Customer() {
		super();
	}

	public Customer(String name, char sex, String username, String company) {
		super( name, sex, username );
		this.company = company;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}
}

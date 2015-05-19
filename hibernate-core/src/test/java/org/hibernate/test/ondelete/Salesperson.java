/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Salesperson.java 7119 2005-06-12 22:03:30Z oneovthafew $
package org.hibernate.test.ondelete;
import java.util.HashSet;
import java.util.Set;

public class Salesperson extends Employee {
	private Set customers = new HashSet();

	public Set getCustomers() {
		return customers;
	}

	public void setCustomers(Set customers) {
		this.customers = customers;
	}

}

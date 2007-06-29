//$Id$
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

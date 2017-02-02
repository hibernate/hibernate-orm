/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idclass;

import java.io.Serializable;

public class CustomerId implements Serializable {
	private String orgName;
	private String customerName;

	public CustomerId() {
		super();
	}

	public CustomerId(String orgName, String custName) {
		this.orgName = orgName;
		this.customerName = custName;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

}

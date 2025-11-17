/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "Customer")
@SecondaryTable(name = "CustomerDetails")
public class Customer extends LegalEntity {

	public String customerName;
	public String customerCode;

	@Column
	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String val) {
		this.customerName = val;
	}

	@Column(table="CustomerDetails")
	public String getCustomerCode() {
		return customerCode;
	}

	public void setCustomerCode(String val) {
		this.customerCode = val;
	}
}

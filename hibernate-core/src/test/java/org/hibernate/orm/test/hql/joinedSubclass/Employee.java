/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.joinedSubclass;

import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class Employee extends Person {
	private String employeeNumber;

	public Employee() {
	}

	public Employee(String name) {
		super( name );
	}

	public Employee(String name, String employeeNumber) {
		super( name );
		this.employeeNumber = employeeNumber;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}
}

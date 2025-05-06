/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;
import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public class EmployerId implements Serializable {
	String name; // matches name of @Id attribute
	long employee; // matches name of @Id attribute and type of Employee PK

	public EmployerId() {
	}

	public EmployerId(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setEmployee(long employee) {
		this.employee = employee;
	}
}

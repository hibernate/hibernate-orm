/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

/**
 * @author Hardy Ferentschik
 */
@Entity
@IdClass(EmployerId.class)
public class Employer {
	@Id
	String name;

	@Id
	@ManyToOne
	Employee employee;

	public Employer() {
	}

	public Employer(String name) {
		this.name = name;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee emp) {
		this.employee = emp;
	}

	public String getName() {
		return name;
	}
}

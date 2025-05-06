/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.notfound;

/**
 * @author Steve Ebersole
 */
public class Person2 {
	private Integer id;
	private String name;
	private Employee2 employee;

	public Person2() {
	}

	public Person2(Integer id, String name, Employee2 employee) {
		this.id = id;
		this.name = name;
		this.employee = employee;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Employee2 getEmployee() {
		return employee;
	}

	public void setEmployee(Employee2 employee) {
		this.employee = employee;
	}
}

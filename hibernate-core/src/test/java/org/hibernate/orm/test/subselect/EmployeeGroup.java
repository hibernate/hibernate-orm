/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;


import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
public class EmployeeGroup {
	@Id
	private EmployeeGroupId id;

	@OneToMany(cascade = CascadeType.ALL)
	@Fetch(FetchMode.SUBSELECT)
	private List<Employee> employees = new ArrayList<Employee>();

	public EmployeeGroup(EmployeeGroupId id) {
		this.id = id;
	}

	@SuppressWarnings("unused")
	private EmployeeGroup() {
	}

	public boolean addEmployee(Employee employee) {
		return employees.add(employee);
	}

	public List<Employee> getEmployees() {
		return employees;
	}

	public EmployeeGroupId getId() {
		return id;
	}

	@Override
	public String toString() {
		return id.toString();
	}
}

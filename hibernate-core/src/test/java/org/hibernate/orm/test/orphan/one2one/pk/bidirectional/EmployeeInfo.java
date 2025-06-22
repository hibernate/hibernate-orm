/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.pk.bidirectional;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EmployeeInfo {
	private Long id;
	private Employee employee;

	public EmployeeInfo() {
	}

	public EmployeeInfo(Employee employee) {
		this.employee = employee;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
}

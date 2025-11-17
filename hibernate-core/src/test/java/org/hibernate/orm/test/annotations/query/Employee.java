/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Employee {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	private Employeegroup employeegroup;

	@ManyToOne
	private Attrset attrset;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employeegroup getEmployeegroup() {
		return employeegroup;
	}

	public void setEmployeegroup(Employeegroup employeegroup) {
		this.employeegroup = employeegroup;
	}

	public Attrset getAttrset() {
		return attrset;
	}

	public void setAttrset(Attrset attrset) {
		this.attrset = attrset;
	}

}

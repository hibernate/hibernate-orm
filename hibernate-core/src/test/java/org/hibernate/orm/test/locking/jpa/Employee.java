/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "employees")
public class Employee {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Basic
	private float salary;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "dept_fk")
	private Department department;

	protected Employee() {
		// for Hibernate use
	}

	public Employee(Integer id, String name, float salary, Department department) {
		this.id = id;
		this.name = name;
		this.salary = salary;
		this.department = department;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getSalary() {
		return salary;
	}

	public void setSalary(float salary) {
		this.salary = salary;
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}
}

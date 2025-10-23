/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

@Entity(name = "Employee")
public class Employee {
	private UUID uniqueIdentifier;

	private String firstName;

	private String lastName;

	private float salary;

	private Company company;

	public Employee() {
	}

	public Employee(UUID uniqueIdentifier, String firstName, String lastName, float salary, Company company) {
		this.uniqueIdentifier = uniqueIdentifier;
		this.firstName = firstName;
		this.lastName = lastName;
		this.salary = salary;
		this.company = company;
	}

	@Id
	public UUID getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	public void setUniqueIdentifier(UUID uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public float getSalary() {
		return salary;
	}

	public void setSalary(float salary) {
		this.salary = salary;
	}

	@ManyToOne
	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}
}

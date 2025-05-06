/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class PhoneNumber {
	int phNumber;
	Collection<Employee> employees;

	@Id
	public int getPhNumber() {
		return phNumber;
	}

	public void setPhNumber(int phNumber) {
		this.phNumber = phNumber;
	}

	@ManyToMany(mappedBy="contactInfo.phoneNumbers", cascade= CascadeType.ALL)
	public Collection<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(Collection<Employee> employees) {
		this.employees = employees;
	}
}

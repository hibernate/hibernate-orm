/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddedid;

import jakarta.persistence.Embeddable;

@Embeddable
public class EmployeeId {
	private String employeeCode;
	private PersonId personId;

	public EmployeeId() {
	}

	public EmployeeId(String employeeCode, PersonId personId) {
		this.employeeCode = employeeCode;
		this.personId = personId;
	}

	public String getEmployeeCode() {
		return employeeCode;
	}

	public PersonId getPersonId() {
		return personId;
	}
}

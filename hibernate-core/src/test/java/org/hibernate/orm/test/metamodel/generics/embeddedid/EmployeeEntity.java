/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddedid;

import jakarta.persistence.Entity;

@Entity
public class EmployeeEntity extends BaseEntity<EmployeeId> {
	private Integer employeeNumber;

	public EmployeeEntity() {
	}

	public EmployeeEntity(EmployeeId id, String name, Integer employeeNumber) {
		super( id, name );
		this.employeeNumber = employeeNumber;
	}
}

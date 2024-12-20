/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh18829;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass(EmployeeWithIdClass.EmployeeId.class)
public class EmployeeWithIdClass extends Address {
	@Id
	String empName;
	@Id
	Integer empId;

	public record EmployeeId(String empName, Integer empId) {
	}

}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh18829;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class EmployeeWithoutIdClass {
	@Id
	String empName;
	@Id
	Integer empId;
	String address;
}

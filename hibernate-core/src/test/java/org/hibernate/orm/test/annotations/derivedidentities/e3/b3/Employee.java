/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;


@Entity
public class Employee {
	@EmbeddedId
	EmployeeId empId;
}

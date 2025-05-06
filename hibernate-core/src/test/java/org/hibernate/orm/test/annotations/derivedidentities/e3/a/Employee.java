/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.a;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
	@EmbeddedId
	EmployeeId empId;
}

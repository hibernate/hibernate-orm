/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.a;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class DependentId implements Serializable {
	String name;	// matches name of @Id attribute
	EmployeeId emp; // matches name of @Id attribute and type of embedded id of Employee
}

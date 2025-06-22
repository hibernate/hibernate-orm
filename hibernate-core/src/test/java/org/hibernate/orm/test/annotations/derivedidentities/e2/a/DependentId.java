/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e2.a;
import java.io.Serializable;
import jakarta.persistence.Embedded;

/**
 * @author Emmanuel Bernard
 */
public class DependentId implements Serializable {
	String name; // matches name of @Id attribute
	@Embedded
	EmployeeId emp; //matches name of attribute and type of Employee PK
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e2.a;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class EmployeeId implements Serializable {
	String  firstName;
	String lastName;
}

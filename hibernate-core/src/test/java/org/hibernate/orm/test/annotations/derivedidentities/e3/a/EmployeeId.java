/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.a;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class EmployeeId implements Serializable{
	String firstName;
	String lastName;
}

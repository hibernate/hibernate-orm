/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.b;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class PersonId implements Serializable {
	String firstName;
	String lastName;

	public PersonId() {
	}

	public PersonId(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}

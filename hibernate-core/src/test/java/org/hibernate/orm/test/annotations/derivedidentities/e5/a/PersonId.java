/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.a;
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

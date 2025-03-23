/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Person {
	@Id
	private String ssn;

	private Person() {
	}

	public Person(String ssn) {
		this.ssn = ssn;
	}

	public String getSsn() {
		return ssn;
	}
}

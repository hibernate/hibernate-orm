/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.binding;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class Person {
	@Id
	String name;
	Address address;

	Person() {}

	Person(String name, Address address) {
		this.name = name;
		this.address = address;
	}
}

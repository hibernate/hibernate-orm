/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class TestEntity {
	@Id
	private int id;

	@OneToMany
	private List<Person> people;

	@ElementCollection
	private List<Address> addresses;

	@ElementCollection
	private List<String> elementCollection;
}

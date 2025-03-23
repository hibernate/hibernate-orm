/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Employee {
	@Id
	private Integer id;

	@Convert(converter = AgeConverter.class)
	private String age;

	public Employee() {
	}

	public Employee(int id, String age) {
		this.id = id;
		this.age = age;
	}

	public int getId() {
		return id;
	}

	public String getAge() {
		return age;
	}
}

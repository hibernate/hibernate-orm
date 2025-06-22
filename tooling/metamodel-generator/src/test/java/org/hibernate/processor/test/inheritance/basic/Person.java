/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.basic;

import jakarta.persistence.Entity;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Person extends AbstractEntity<String> {
	private String name;

	protected Person() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

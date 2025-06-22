/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;


/**
 * @author Gavin King
 */
public class Category {

	private String name;

	Category() {}

	public Category(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}

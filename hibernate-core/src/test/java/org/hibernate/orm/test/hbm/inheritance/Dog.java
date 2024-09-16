/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.inheritance;

public class Dog extends Animal {

	private DogName name;

	public DogName getName() {
		return name;
	}

	public void setName(DogName name) {
		this.name = name;
	}
}

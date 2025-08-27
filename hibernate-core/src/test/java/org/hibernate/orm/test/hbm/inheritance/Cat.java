/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.inheritance;

public class Cat extends Animal {

	private CatName name;

	public CatName getName() {
		return name;
	}

	public void setName(CatName name) {
		this.name = name;
	}
}

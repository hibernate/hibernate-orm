/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.mappingexception;

public class Person {

	private long id;
	private boolean name;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isName() {
		return name;
	}

	public void setName(boolean name) {
		this.name = name;
	}
}

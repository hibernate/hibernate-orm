/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

public class Custom implements Cloneable {
	String id;
	private String name;

	public Object clone() {
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException cnse) {
			throw new RuntimeException();
		}
	}

	void setName(String name) {
		this.name = name;
	}

	String getName() {
		return name;
	}

}

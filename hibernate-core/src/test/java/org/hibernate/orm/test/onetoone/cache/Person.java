/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.cache;

import java.io.Serializable;

public abstract class Person implements Serializable {
	private int id;
	private String name;
	private Details details;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Details getDetails() {
		return details;
	}

	public void setDetails(Details details) {
		if (details != null) {
			details.setPerson(this);
		}

		this.details = details;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

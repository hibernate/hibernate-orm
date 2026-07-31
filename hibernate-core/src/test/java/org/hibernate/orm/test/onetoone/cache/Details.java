/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.cache;

import java.io.Serializable;

public abstract class Details implements Serializable {
	private String data;
	private Person person;

	abstract public int getId();

	abstract public void setId(int id);

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Person getPerson() {
		return person;
	}

	protected void setPerson(Person person) {
		this.person = person;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.cache;

import java.io.Serializable;

public abstract class Details implements Serializable {
	private int id;
	private String data;
	private Person person;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;


public class Tag {
	private int id;
	private String surrogate;

	public Tag() {

	}

	public Tag(String surrogate) {
		this.surrogate = surrogate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSurrogate() {
		return surrogate;
	}

	public void setSurrogate(String surrogate) {
		this.surrogate = surrogate;
	}

}

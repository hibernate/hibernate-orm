/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 *
 * @author Gail Badner
 */
public class EntityWithArgFunctionAsColumn {
	private long id;
	private int lower;
	private String upper;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public int getLower() {
		return lower;
	}
	public void setLower(int lower) {
		this.lower = lower;
	}

	public String getUpper() {
		return upper;
	}
	public void setUpper(String upper) {
		this.upper = upper;
	}
}

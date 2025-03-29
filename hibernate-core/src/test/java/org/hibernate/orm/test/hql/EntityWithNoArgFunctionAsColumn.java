/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 *
 * @author Gail Badner
 */
public class EntityWithNoArgFunctionAsColumn {
	private long id;
	private String user;
	private String currentDate;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}

	public String getCurrentDate() {
		return currentDate;
	}
	public void setCurrentDate(String currentDate) {
		this.currentDate = currentDate;
	}
}

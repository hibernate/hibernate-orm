/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.propertyref.lazy;


public class Mail {

	private Integer id;
	private String alias;
	private User user;

	Mail() {
	}

	Mail(String alias, User user) {
		this.alias = alias;
		this.user = user;
	}

	public Integer getId() {
		return id;
	}

	protected void setId(Integer id) {
		this.id = id;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}

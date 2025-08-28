/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class User {
	private String name;
	private String password;
	private Set actions = new HashSet();
	private Calendar lastUpdated;
	private Calendar created;
	private String injectedString;

	public User(String name, String password) {
		super();
		this.name = name;
		this.password = password;
	}
	public User() {
		super();
	}
	public Calendar getLastUpdated() {
		return lastUpdated;
	}
	public void setLastUpdated(Calendar lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Set getActions() {
		return actions;
	}
	public void setActions(Set actions) {
		this.actions = actions;
	}
	public Calendar getCreated() {
		return created;
	}
	public void setCreated(Calendar created) {
		this.created = created;
	}
	public String getInjectedString() {
		return injectedString;
	}
	public void setInjectedString(String injectedString) {
		this.injectedString = injectedString;
	}
}

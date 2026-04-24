/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.stat.Statistics;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Group {
	private String name;
	private Set<User> users = new HashSet<User>();
	Group() {}
	public Group(String n) {
		name = n;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<User> getUsers() {
		return users;
	}
	public void setUsers(Set<User> users) {
		this.users = users;
	}
	public void addUser(User user) {
		users.add(user);

	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.ManyToManyTest;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Group implements Serializable {

	private static final long serialVersionUID =
			ObjectStreamClass.lookup(Group.class).getSerialVersionUID();

	private String org;
	private String name;
	private String description;

	private Set<User> users = new HashSet<User>();

	public Group(String name, String org) {
		this.org = org;
		this.name = name;
	}

	public Group() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

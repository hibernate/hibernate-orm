/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.query.QueryExporterTest;

import java.io.ObjectStreamClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Group implements Serializable {

	@Serial
	private static final long serialVersionUID =
			ObjectStreamClass.lookup(Group.class).getSerialVersionUID();

	private String description;
	private UserID userId;

	private Set<User> users = new HashSet<>();

	public Group(String name, String org) {
		this.setUserId(new UserID(name, org));
	}

	public Group() {
	}

	public void setUserId(UserID userId) {
		this.userId = userId;
	}

	public UserID getUserId() {
		return userId;
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

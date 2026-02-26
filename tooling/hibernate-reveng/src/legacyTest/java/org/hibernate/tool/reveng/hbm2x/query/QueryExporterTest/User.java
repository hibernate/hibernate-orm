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

public class User implements Serializable {

	@Serial
	private static final long serialVersionUID =
			ObjectStreamClass.lookup(User.class).getSerialVersionUID();

	private Set<Group> groups = new HashSet<>();
	private UserID userId;

	public User(String name, String org) {
		this.setUserId(new UserID(name, org));
	}

	public User() {
	}

	public void setUserId(UserID userId) {
		this.userId = userId;
	}

	public UserID getUserId() {
		return userId;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import java.util.HashMap;
import java.util.Map;

public class Group {
	private String name;
	private Map users = new HashMap();

	public Group(String name) {
		this.name = name;
	}

	Group() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map getUsers() {
		return users;
	}

	public void setUsers(Map users) {
		this.users = users;
	}

}

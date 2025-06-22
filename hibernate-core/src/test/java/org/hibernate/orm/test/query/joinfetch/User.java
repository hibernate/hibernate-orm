/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import java.util.HashMap;
import java.util.Map;

public class User {
	private String name;
	private Map groups = new HashMap();

	public User(String name) {
		this.name = name;
	}

	User() {}

	public Map getGroups() {
		return groups;
	}

	public void setGroups(Map groups) {
		this.groups = groups;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

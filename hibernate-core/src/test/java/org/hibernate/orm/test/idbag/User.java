/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idbag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class User {
	private String name;
	private List groups = new ArrayList();

	User() {}

	public User(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}


	void setName(String name) {
		this.name = name;
	}

	public List getGroups() {
		return groups;
	}

	void setGroups(List groups) {
		this.groups = groups;
	}

}

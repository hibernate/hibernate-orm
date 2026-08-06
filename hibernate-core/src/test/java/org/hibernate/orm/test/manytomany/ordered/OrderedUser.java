/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.ordered;

import java.util.ArrayList;
import java.util.List;

public class OrderedUser {
	private String name;
	private List<OrderedGroup> groups = new ArrayList<>();

	public OrderedUser() {
	}

	public OrderedUser(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<OrderedGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<OrderedGroup> groups) {
		this.groups = groups;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.collections.list;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class Root {
	private Integer id;
	private String name;
	private List<String> tags;
	private List<Category> categories;
	private List<User> admins;
	private List<User> admins2;


	protected Root() {
		// for Hibernate use
	}

	public Root(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}

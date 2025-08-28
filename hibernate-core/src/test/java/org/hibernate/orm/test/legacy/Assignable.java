/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.Collection;

/**
 * @author Administrator
 */
public class Assignable {
	private String id;
	private Collection categories;

	public Collection getCategories() {
		return categories;
	}

	public String getId() {
		return id;
	}

	public void setCategories(Collection collection) {
		categories = collection;
	}

	public void setId(String string) {
		id = string;
	}

}

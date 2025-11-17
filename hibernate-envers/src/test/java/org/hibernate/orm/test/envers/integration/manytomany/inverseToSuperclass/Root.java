/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.inverseToSuperclass;

import java.util.List;

import org.hibernate.envers.Audited;

@Audited
public class Root {

	private long id;

	private String str;

	private List<DetailSubclass> items;

	public Root() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public List<DetailSubclass> getItems() {
		return items;
	}

	public void setItems(List<DetailSubclass> items) {
		this.items = items;
	}

}

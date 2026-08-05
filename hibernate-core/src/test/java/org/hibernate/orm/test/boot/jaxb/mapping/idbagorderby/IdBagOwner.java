/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.idbagorderby;

import java.util.ArrayList;
import java.util.List;

public class IdBagOwner {
	private String name;
	private List<IdBagItem> items = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<IdBagItem> getItems() {
		return items;
	}

	public void setItems(List<IdBagItem> items) {
		this.items = items;
	}
}

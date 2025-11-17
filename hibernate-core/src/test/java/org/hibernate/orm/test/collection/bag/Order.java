/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gail Badner
 */
public class Order {
	private Long id;

	private List<Item> items = new ArrayList<>();

	public Order() {
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public List<Item> getItems() {
		return items;
	}
	public void setItems(List<Item> items) {
		this.items = items;
	}

	public void addItem(Item item) {
		items.add( item );
		item.setOrder( this );
	}
}

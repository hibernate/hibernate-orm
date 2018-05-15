/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.bag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gail Badner
 */
public class Order {
	private Long id;

	private List<Item> items = new ArrayList<Item>();

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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.PersistentClasses;
import java.util.HashSet;
import java.util.Set;

/**
 * @author max
 *
 */
public class Orders {

	Integer id;

	String name;

	Set<Item> items = new HashSet<>();
	Set<Item> items_1 = new HashSet<>();


	/**
	 * @return Returns the id.
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id
	 *            The id to set.
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the setOfItem.
	 */
	public Set<Item> getItemsForOrderId() {
		return items;
	}

	/**
	 * @param items
	 *            The setOfItem to set.
	 */
	public void setItemsForOrderId(Set<Item> items) {
		this.items = items;
	}
	/**
	 * @return Returns the setOfItem_1.
	 */
	public Set<Item> getItemsForRelatedOrderId() {
		return items_1;
	}
	/**
	 * @param items_1 The setOfItem_1 to set.
	 */
	public void setItemsForRelatedOrderId(Set<Item> items_1) {
		this.items_1 = items_1;
	}
}

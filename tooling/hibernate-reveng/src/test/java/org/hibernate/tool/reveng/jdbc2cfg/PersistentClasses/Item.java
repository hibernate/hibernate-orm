/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.PersistentClasses;
public class Item {

	Integer childId;

	Orders order;
	Orders relatedorderId;
	String name;

	/**
	 * @return Returns the id.
	 */
	public Integer getChildId() {
		return childId;
	}
	/**
	 * @param id The id to set.
	 */
	public void setChildId(Integer id) {
		this.childId = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the order.
	 */
	public Orders getOrderId() {
		return order;
	}
	/**
	 * @param order The order to set.
	 */
	public void setOrderId(Orders order) {
		this.order = order;
	}
	/**
	 * @return Returns the order.
	 */
	public Orders getOrdersByOrderId() {
		return order;
	}
	/**
	 * @param order The order to set.
	 */
	public void setOrdersByOrderId(Orders order) {
		this.order = order;
	}
	/**
	 * @return Returns the relatedorderId.
	 */
	public Orders getOrdersByRelatedOrderId() {
		return relatedorderId;
	}
	/**
	 * @param relatedorderId The relatedorderId to set.
	 */
	public void setOrdersByRelatedOrderId(Orders relatedorderId) {
		this.relatedorderId = relatedorderId;
	}
}

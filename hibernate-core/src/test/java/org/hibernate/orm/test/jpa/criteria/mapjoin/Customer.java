/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.mapjoin;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Customer {

	@Id
	@GeneratedValue
	private int id;
	private String name;
	@OneToMany(cascade = CascadeType.ALL)
	private Map<String, CustomerOrder> orderMap;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, CustomerOrder> getOrderMap() {
		return orderMap;
	}

	public void setOrderMap(Map<String, CustomerOrder> orderMap) {
		this.orderMap = orderMap;
	}

	public void addOrder(String orderType, String itemName, int qty) {
		if ( orderMap == null ) {
			orderMap = new HashMap<>();
		}
		CustomerOrder order = new CustomerOrder();
		order.setItem( itemName );
		order.setQty( qty );
		orderMap.put( orderType, order );
	}

	@Override
	public String toString() {
		return "Customer{" +
				"id=" + id +
				", name='" + name + '\'' +
				", orderMap=" + orderMap +
				'}';
	}
}

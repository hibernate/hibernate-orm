/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.query;

public class OrderId {
	Integer customer;
	Integer orderNumber;

	public OrderId() {
	}

	public OrderId(Integer customer, Integer orderNumber) {
		this.customer = customer;
		this.orderNumber = orderNumber;
	}
}

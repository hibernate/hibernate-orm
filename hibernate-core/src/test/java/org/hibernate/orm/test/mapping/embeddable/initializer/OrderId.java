/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.initializer;

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

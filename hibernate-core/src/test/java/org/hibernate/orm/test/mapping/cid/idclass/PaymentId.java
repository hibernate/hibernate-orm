/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.idclass;

/**
 * @author Steve Ebersole
 */
public class PaymentId {
	private OrderId order;
	private String accountNumber;

	public PaymentId() {
	}

	public PaymentId(OrderId order, String accountNumber) {
		this.order = order;
		this.accountNumber = accountNumber;
	}
}

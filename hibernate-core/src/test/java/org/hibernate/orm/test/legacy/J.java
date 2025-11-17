/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author Gavin King
 */
public class J extends I {
	private float amount;

	void setAmount(float amount) {
		this.amount = amount;
	}

	float getAmount() {
		return amount;
	}
}

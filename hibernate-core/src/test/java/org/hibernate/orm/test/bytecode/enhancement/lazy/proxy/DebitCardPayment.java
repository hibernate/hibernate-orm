/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "DebitCardPayment")
@Table(name = "debit_payment")
public class DebitCardPayment extends Payment {
	private String transactionId;

	public DebitCardPayment() {
	}

	public DebitCardPayment(Integer oid, Float amount, String transactionId) {
		super( oid, amount );
		this.transactionId = transactionId;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
}

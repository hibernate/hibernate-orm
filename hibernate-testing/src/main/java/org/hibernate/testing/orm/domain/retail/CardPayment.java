/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import javax.money.Monetary;
import javax.money.MonetaryAmount;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Steve Ebersole
 */
@Entity
@PrimaryKeyJoinColumn(name = "payment_fk", referencedColumnName = "id")
public class CardPayment extends Payment {
	private Integer transactionId;

	public CardPayment() {
	}

	public CardPayment(Integer id, Integer transactionId, MonetaryAmount amount) {
		super( id,amount );
		this.transactionId = transactionId;
	}

	public CardPayment(Integer id, Integer transactionId, Number amount, String currencyCode) {
		super( id, Monetary.getDefaultAmountFactory().setNumber( amount ).setCurrency( currencyCode ).create() );
		this.transactionId = transactionId;
	}

	public CardPayment(Integer id, Integer transactionId, Long amount, String currencyCode) {
		super( id, Monetary.getDefaultAmountFactory().setNumber( amount ).setCurrency( currencyCode ).create() );
		this.transactionId = transactionId;
	}

	public Integer getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Integer transactionId) {
		this.transactionId = transactionId;
	}
}

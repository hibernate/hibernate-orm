/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class CashPayment extends Payment {
	public CashPayment() {
	}

	public CashPayment(Integer id, MonetaryAmount amount) {
		super( id, amount );
	}

	public CashPayment(Integer id, Long amount, String currencyCode) {
		super( id, Monetary.getDefaultAmountFactory().setNumber( amount ).setCurrency( currencyCode ).create() );
	}
}

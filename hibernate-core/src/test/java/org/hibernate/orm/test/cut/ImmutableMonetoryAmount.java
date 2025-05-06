/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

public class ImmutableMonetoryAmount implements Serializable {

	private BigDecimal amount;
	private Currency currency;

	public ImmutableMonetoryAmount(BigDecimal amount, Currency currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}


	public Currency getCurrency() {
		return currency;
	}

}

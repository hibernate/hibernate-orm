/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chris Pheby
 */
public class DollarValue implements Serializable {

	private static final long serialVersionUID = -416056386419355705L;

	private BigDecimal amount;

	public DollarValue() {};

	public DollarValue(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getAmount() {
		return amount;
	}
}

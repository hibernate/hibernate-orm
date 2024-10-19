/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.mixed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity(name = "CashPayment")
public class CashPayment implements Payment {
	@Id
	private Integer id;
	private Double amount;

	public CashPayment() {
	}

	public CashPayment(Integer id, Double amount) {
		this.id = id;
		this.amount = amount;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}
}

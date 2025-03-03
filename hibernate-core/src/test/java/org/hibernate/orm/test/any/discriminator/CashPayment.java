/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
//tag::associations-any-example[]
@Entity
public class CashPayment implements Payment {
	// ...
//end::associations-any-example[]
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
//tag::associations-any-example[]
}
//end::associations-any-example[]

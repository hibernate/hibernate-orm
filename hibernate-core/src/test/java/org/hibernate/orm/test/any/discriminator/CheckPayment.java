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
public class CheckPayment implements Payment {
	// ...
//end::associations-any-example[]
	@Id
	public Integer id;
	public Double amount;
	public int checkNumber;
	public String routingNumber;
	public String accountNumber;

	public CheckPayment() {
	}

	public CheckPayment(Integer id, Double amount, int checkNumber, String routingNumber, String accountNumber) {
		this.id = id;
		this.amount = amount;
		this.checkNumber = checkNumber;
		this.routingNumber = routingNumber;
		this.accountNumber = accountNumber;
	}

	@Override
	public Double getAmount() {
		return amount;
	}
//tag::associations-any-example[]
}
//end::associations-any-example[]

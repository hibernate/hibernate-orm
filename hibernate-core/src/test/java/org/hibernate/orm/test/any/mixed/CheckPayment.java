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
@Entity
public class CheckPayment implements Payment {
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
}

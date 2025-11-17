/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.idclass; /**
 * @author Steve Ebersole
 */

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "Payment")
@Table(name = "payments")
@IdClass( PaymentId.class )
public class Payment {
	@Id
	@ManyToOne
	public Order order;

	@Basic
	public String accountNumber;

	protected Payment() {
		// for Hibernate use
	}

	public Payment(Order order, String accountNumber) {
		this.order = order;
		this.accountNumber = accountNumber;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}
}

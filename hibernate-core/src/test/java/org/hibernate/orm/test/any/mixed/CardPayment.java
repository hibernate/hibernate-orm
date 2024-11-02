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
@Entity(name = "CardPayment")
public class CardPayment implements Payment {
	@Id
	private Integer id;
	private Double amount;
	private String authorizationCode;

	public CardPayment() {
	}

	public CardPayment(Integer id, Double amount, String authorizationCode) {
		this.id = id;
		this.amount = amount;
		this.authorizationCode = authorizationCode;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getAuthorizationCode() {
		return authorizationCode;
	}

	public void setAuthorizationCode(String authorizationCode) {
		this.authorizationCode = authorizationCode;
	}
}

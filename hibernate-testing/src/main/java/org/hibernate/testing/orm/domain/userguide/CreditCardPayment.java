/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Vlad Mihalcea
 */
//tag::hql-examples-domain-model-example[]
@Entity
public class CreditCardPayment extends Payment {
	@Column(name = "card_number")
	String cardNumber;

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getCardNumber() {
		return cardNumber;
	}
}
//end::hql-examples-domain-model-example[]

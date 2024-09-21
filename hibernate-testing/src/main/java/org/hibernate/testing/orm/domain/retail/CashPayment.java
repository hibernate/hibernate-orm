/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

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
}

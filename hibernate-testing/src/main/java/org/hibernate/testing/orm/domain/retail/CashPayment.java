/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import javax.money.MonetaryAmount;

import javax.persistence.Entity;

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

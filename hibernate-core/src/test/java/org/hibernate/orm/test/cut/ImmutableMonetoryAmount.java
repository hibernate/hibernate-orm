/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cut;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

public class ImmutableMonetoryAmount implements Serializable {

	private BigDecimal amount;
	private Currency currency;

	public ImmutableMonetoryAmount(BigDecimal amount, Currency currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}


	public Currency getCurrency() {
		return currency;
	}

}

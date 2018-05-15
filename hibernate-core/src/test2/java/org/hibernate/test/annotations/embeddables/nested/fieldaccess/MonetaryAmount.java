/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables.nested.fieldaccess;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * @author Thomas Vanstals
 * @author Steve Ebersole
 */
@Embeddable
public class MonetaryAmount {
	public static enum CurrencyCode {
		USD,
		EUR
	}

	private BigDecimal amount;

	@Column(length = 3)
	@Enumerated(EnumType.STRING)
	private CurrencyCode currency;

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public CurrencyCode getCurrency() {
		return currency;
	}

	public void setCurrency(CurrencyCode currency) {
		this.currency = currency;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chris Pheby
 */
public class DollarValue implements Serializable {

	private static final long serialVersionUID = -416056386419355705L;

	private BigDecimal amount;

	public DollarValue() {};
	
	public DollarValue(BigDecimal amount) {
		this.amount = amount;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
}

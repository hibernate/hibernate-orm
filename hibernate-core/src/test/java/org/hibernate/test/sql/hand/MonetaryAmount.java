/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Represents a monetary amount as value and currency.
 *
 * @author Gavin King <gavin@hibernate.org>
 * @author Christian Bauer <christian@hibernate.org>
 */
public class MonetaryAmount implements Serializable {

	private final BigDecimal value;
	private final Currency currency;

	public MonetaryAmount(BigDecimal value, Currency currency) {
		this.value = value;
		this.currency = currency;
	}

	public Currency getCurrency() {
		return currency;
	}

	public BigDecimal getValue() {
		return value;
	}

	// ********************** Common Methods ********************** //

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MonetaryAmount)) return false;

		final MonetaryAmount monetaryAmount = (MonetaryAmount) o;

		if (!currency.equals(monetaryAmount.currency)) return false;
		if (!value.equals(monetaryAmount.value)) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = value.hashCode();
		result = 29 * result + currency.hashCode();
		return result;
	}

	public String toString() {
		return "Value: '" + getValue() + "', " +
		        "Currency: '" + getCurrency() + "'";
	}

	public int compareTo(Object o) {
		if (o instanceof MonetaryAmount) {
			// TODO: This would actually require some currency conversion magic
			return this.getValue().compareTo(((MonetaryAmount) o).getValue());
		}
		return 0;
	}

	// ********************** Business Methods ********************** //

	public static MonetaryAmount fromString(String amount, String currencyCode) {
		return new MonetaryAmount(new BigDecimal(amount),
								  Currency.getInstance(currencyCode));
	}

	public static MonetaryAmount convert(MonetaryAmount amount,
										 Currency toConcurrency) {
		// TODO: This requires some conversion magic and is therefore broken
		return new MonetaryAmount(amount.getValue(), toConcurrency);
	}

}

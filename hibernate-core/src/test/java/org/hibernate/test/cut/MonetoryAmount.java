//$Id: MonetoryAmount.java 6234 2005-03-29 03:07:30Z oneovthafew $
package org.hibernate.test.cut;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * @author Gavin King
 */
public class MonetoryAmount implements Serializable {

	private BigDecimal amount;
	private Currency currency;
	
	public MonetoryAmount(BigDecimal amount, Currency currency) {
		this.amount = amount;
		this.currency = currency;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

}

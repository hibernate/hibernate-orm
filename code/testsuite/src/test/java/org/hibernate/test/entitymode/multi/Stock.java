// $Id: Stock.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.entitymode.multi;

import java.util.Set;
import java.util.HashSet;

/**
 * POJO implementation of Stock entity.
 *
 * @author Steve Ebersole
 */
public class Stock {
	private Long id;
	private String tradeSymbol;
	private Valuation currentValuation;
	private Set valuations = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTradeSymbol() {
		return tradeSymbol;
	}

	public void setTradeSymbol(String tradeSymbol) {
		this.tradeSymbol = tradeSymbol;
	}

	public Valuation getCurrentValuation() {
		return currentValuation;
	}

	public void setCurrentValuation(Valuation currentValuation) {
		this.currentValuation = currentValuation;
	}

	public Set getValuations() {
		return valuations;
	}

	public void setValuations(Set valuations) {
		this.valuations = valuations;
	}
}

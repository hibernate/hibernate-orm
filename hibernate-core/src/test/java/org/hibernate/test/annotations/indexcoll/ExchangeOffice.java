//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.math.BigDecimal;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.CascadeType;


@Entity
public class ExchangeOffice {
	public ExchangeOffice() {
		super();
	}

	@Id @GeneratedValue
	private Integer id;
	

	public void setId(Integer id) {
		this.id = id;
	}

	
	public Integer getId() {
		return id;
	}

	@javax.persistence.OneToMany(mappedBy = "parent")
    @javax.persistence.MapKey(name="key")
    private Map<ExchangeRateKey, ExchangeRate> exchangeRates = new java.util.HashMap<ExchangeRateKey, ExchangeRate>();
	
	public Map<ExchangeRateKey,ExchangeRate> getExchangeRates() {
		return exchangeRates;
	}

	@ElementCollection
	private Map<ExchangeRateKey, BigDecimal> exchangeRateFees = new java.util.HashMap<ExchangeRateKey, BigDecimal>();

	public Map<ExchangeRateKey,BigDecimal> getExchangeRateFees() {
		return exchangeRateFees;
	}
	
}

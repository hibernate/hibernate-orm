//$Id$
package org.hibernate.test.annotations.manytoone;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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
    @org.hibernate.annotations.OptimisticLock(excluded=false)
    @javax.persistence.MapKey(name="key")
    @org.hibernate.annotations.Cascade( value = CascadeType.LOCK)
    protected Map<ExchangeRateKey, ExchangeRate> exchangeRates = new java.util.HashMap<ExchangeRateKey, ExchangeRate>();
	
	public Map<ExchangeRateKey,ExchangeRate> getExchangeRates() {
		return exchangeRates;
	}

	
}

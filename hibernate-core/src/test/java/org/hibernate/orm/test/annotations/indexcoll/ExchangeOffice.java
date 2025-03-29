/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.math.BigDecimal;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


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

	@jakarta.persistence.OneToMany(mappedBy = "parent")
	@jakarta.persistence.MapKey(name="key")
	private Map<ExchangeRateKey, ExchangeRate> exchangeRates = new java.util.HashMap<ExchangeRateKey, ExchangeRate>();

	public Map<ExchangeRateKey,ExchangeRate> getExchangeRates() {
		return exchangeRates;
	}

	@ElementCollection
	@CollectionTable(name = "ExchangeO_exchangeRateFees")
	private Map<ExchangeRateKey, BigDecimal> exchangeRateFees = new java.util.HashMap<ExchangeRateKey, BigDecimal>();

	public Map<ExchangeRateKey,BigDecimal> getExchangeRateFees() {
		return exchangeRateFees;
	}

}

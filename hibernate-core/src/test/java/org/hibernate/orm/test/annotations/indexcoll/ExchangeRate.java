/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


@Entity
public class ExchangeRate {
	public ExchangeRate() {
		super();
	}

	@Id @GeneratedValue
	private Integer id;

	@Column
	private double rate;



	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
	private ExchangeRateKey key = new ExchangeRateKey();

	public ExchangeRateKey getKey() {
		return key;
	}

	public void setKey(ExchangeRateKey key) {
		this.key = key;
	}

	@jakarta.persistence.ManyToOne(fetch = FetchType.LAZY )
	private ExchangeOffice parent = null;


	public ExchangeOffice getParent() {
		return parent;
	}

	public void setParent(ExchangeOffice parent) {
		this.parent = parent;
	}
}

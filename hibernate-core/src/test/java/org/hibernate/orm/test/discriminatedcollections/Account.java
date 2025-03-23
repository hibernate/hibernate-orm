/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.discriminatedcollections;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorColumn(name = "account_type")
abstract class Account {

	@Id
	private Integer id;
	private double amount;
	private Double rate;

	@ManyToOne(fetch = FetchType.LAZY)
	private Client client;

	Account() {}

	public Account(Integer id, double amount, Double rate, Client client) {
		this.id = id;
		this.amount = amount;
		this.rate = rate;
		this.client = client;
	}

	public Account(Integer id, Client client) {
		this( id, 0.0, 12.0, client );
	}

	public Integer getId() {
		return id;
	}

	public Client getClient() {
		return client;
	}


	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}


	public Double getRate() {
		return rate;
	}

	public void setRate(Double rate) {
		this.rate = rate;
	}

	public abstract AccountType getType();
}

@Entity
@DiscriminatorValue("D")
class DebitAccount extends Account {

	DebitAccount() {
	}

	public DebitAccount(Integer id, double amount, Double rate, Client client) {
		super( id, amount, rate, client );
	}

	public DebitAccount(Integer id, Client client) {
		super( id, client );
	}

	@Override
	public AccountType getType() {
		return AccountType.DEBIT;
	}
}

@Entity
@DiscriminatorValue("C")
class CreditAccount extends Account {

	CreditAccount() {
	}

	public CreditAccount(Integer id, double amount, Double rate, Client client) {
		super( id, amount, rate, client );
	}

	public CreditAccount(Integer id, Client client) {
		super( id, client );
	}

	@Override
	public AccountType getType() {
		return AccountType.CREDIT;
	}
}

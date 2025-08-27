/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter.hbm;

//tag::basic-hbm-attribute-converter-mapping-account-example[]
public class Account {

	private Long id;

	private String owner;

	private Money balance;

	//Getters and setters are omitted for brevity
	//end::basic-hbm-attribute-converter-mapping-account-example[]
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Money getBalance() {
		return balance;
	}

	public void setBalance(Money balance) {
		this.balance = balance;
	}
//tag::basic-hbm-attribute-converter-mapping-account-example[]
}
//end::basic-hbm-attribute-converter-mapping-account-example[]

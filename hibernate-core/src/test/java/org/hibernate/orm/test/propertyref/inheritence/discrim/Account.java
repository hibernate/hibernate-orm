/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref.inheritence.discrim;


/**
 * @author Gavin King
 */
public class Account {
	private String accountId;
	private Customer customer;
	private Person person;
	private char type;
	/**
	 * @return Returns the user.
	 */
	public Customer getCustomer() {
		return customer;
	}
	/**
	 * @param user The user to set.
	 */
	public void setCustomer(Customer user) {
		this.customer = user;
	}
	/**
	 * @return Returns the accountId.
	 */
	public String getAccountId() {
		return accountId;
	}
	/**
	 * @param accountId The accountId to set.
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	/**
	 * @return Returns the type.
	 */
	public char getType() {
		return type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(char type) {
		this.type = type;
	}
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}


}

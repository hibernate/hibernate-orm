/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.keymanytoone.bidir.ondelete;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak
 */
public class Order implements Serializable {
	private Customer customer;
	private long number;
	private String item;

	public Order() {
	}

	public Order(Customer customer, long number) {
		this.customer = customer;
		this.number = number;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "Order")
@Table(name = "orders")
@IdClass( OrderId.class )
public class Order {
	@Id
	@ManyToOne
	public Customer customer;

	@Id
	public Integer orderNumber;

	public Float amount;

	protected Order() {
		// for Hibernate use
	}

	public Order(Customer customer, Integer orderNumber, Float amount) {
		this.customer = customer;
		this.orderNumber = orderNumber;
		this.amount = amount;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Integer getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}

	public Float getAmount() {
		return amount;
	}

	public void setAmount(Float amount) {
		this.amount = amount;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.FetchProfile;

/**
 * @author Hardy Ferentschik
 */
@Entity
@FetchProfile(name = "customer-with-orders", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer.class, association = "orders")
})
@FetchProfile(name = "customer-with-orders-and-country",
		fetchOverrides = {
	@FetchProfile.FetchOverride(entity = Customer.class, association = "orders"),
	@FetchProfile.FetchOverride(entity = Customer.class, association = "lastOrder"),
	@FetchProfile.FetchOverride(entity = Order.class, association = "country")
})
public class Customer {
	@Id
	@GeneratedValue
	private long id;

	private String name;

	private long customerNumber;

	@OneToMany
	private Set<Order> orders = new HashSet<Order>();

	@ManyToOne(fetch = FetchType.LAZY)
	private Order lastOrder;

	public Order getLastOrder() {
		return lastOrder;
	}

	public void setLastOrder(Order lastOrder) {
		this.lastOrder = lastOrder;
	}

	public Set<SupportTickets> getTickets() {
		return tickets;
	}

	public void setTickets(Set<SupportTickets> tickets) {
		this.tickets = tickets;
	}

	@OneToMany
	private Set<SupportTickets> tickets;

	public long getCustomerNumber() {
		return customerNumber;
	}

	public void setCustomerNumber(long customerNumber) {
		this.customerNumber = customerNumber;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Order> getOrders() {
		return orders;
	}

	public void setOrders(Set<Order> orders) {
		this.orders = orders;
	}
}

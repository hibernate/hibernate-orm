/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.test.annotations.fetchprofile;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;

/**
 * @author Hardy Ferentschik
 */
@Entity
@FetchProfile(name = "customer-with-orders", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer.class, association = "orders", mode = FetchMode.JOIN)
})
@FetchProfile(name = "customer-with-orders-and-country",
		fetchOverrides = {
	@FetchProfile.FetchOverride(entity = Customer.class, association = "orders", mode = FetchMode.JOIN),
	@FetchProfile.FetchOverride(entity = Customer.class, association = "lastOrder", mode = FetchMode.JOIN),
	@FetchProfile.FetchOverride(entity = Order.class, association = "country", mode = FetchMode.JOIN)
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



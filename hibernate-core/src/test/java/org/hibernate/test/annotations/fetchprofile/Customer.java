// $Id$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
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
import org.hibernate.annotations.FetchProfiles;

/**
 * @author Hardy Ferentschik
 */
@Entity
@FetchProfiles( {
		@FetchProfile(name = "customer-with-orders", fetchOverrides = {
				@FetchProfile.FetchOverride(entity = Customer.class, association = "orders", mode = FetchMode.JOIN)
		}),
		@FetchProfile(name = "customer-with-orders-and-country",
				fetchOverrides = {
			@FetchProfile.FetchOverride(entity = Customer.class, association = "orders", mode = FetchMode.JOIN),
			@FetchProfile.FetchOverride(entity = Customer.class, association = "lastOrder", mode = FetchMode.JOIN),
			@FetchProfile.FetchOverride(entity = Order.class, association = "country", mode = FetchMode.JOIN)
		})
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



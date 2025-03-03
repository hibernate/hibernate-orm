/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.FetchProfile;


/**
 * @author Hardy Ferentschik
 */
@Entity
@FetchProfile(name = "orders-profile", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer5.class, association = "foo")
})
public class Customer5 {
	@Id
	@GeneratedValue
	private long id;

	private String name;

	private long customerNumber;

	@OneToMany
	private Set<Order> orders;

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

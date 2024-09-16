/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.inverse;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "customers" )
public class Customer {
	@Id
	private Integer id;
	@Basic
	private String name;
	@OneToMany( mappedBy = "customer" )
	private Set<Order> orders;

	protected Customer() {
		// for Hibernate use
	}

	public Customer(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide.tooling;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
//tag::tooling-modelgen-model[]
@Entity
@Table(name = "orders")
public class Order {
	@Id
	Integer id;

	@ManyToOne
	Customer customer;

	@OneToMany
	Set<Item> items;
	BigDecimal totalCost;

	// standard setter/getter methods

//end::tooling-modelgen-model[]

	public Order() {
	}

	public Order(Integer id, Customer customer, BigDecimal totalCost) {
		this.id = id;
		this.customer = customer;
		this.totalCost = totalCost;
	}
//tag::tooling-modelgen-model[]
}
//end::tooling-modelgen-model[]

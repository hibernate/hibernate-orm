/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.inverse;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "orders" )
public class Order {
	@Id
	private Integer id;
	@Basic
	private Instant timestamp;
	@ManyToOne
	@JoinColumn( name = "customer_fk" )
	private Customer customer;

	protected Order() {
		// for Hibernate use
	}

	public Order(Integer id, Instant timestamp) {
		this.id = id;
		this.timestamp = timestamp;
	}

	public Integer getId() {
		return id;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
}

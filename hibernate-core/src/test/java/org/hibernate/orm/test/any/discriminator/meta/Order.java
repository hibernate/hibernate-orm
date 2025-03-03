/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.meta;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.orm.test.any.discriminator.Payment;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "orders")
public class Order {
	@Id
	public Integer id;
	@Basic
	public String name;

	//tag::associations-any-discriminator-meta-example[]
	@Any
	@PaymentDiscriminationDef
	@Column(name = "payment_type")
	@JoinColumn(name = "payment_fk")
	public Payment payment;
	//end::associations-any-discriminator-meta-example[]

	protected Order() {
		// for Hibernate use
	}

	public Order(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.explicit;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.orm.test.any.discriminator.CardPayment;
import org.hibernate.orm.test.any.discriminator.CheckPayment;
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

	//tag::associations-any-explicit-discriminator-example[]
	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "explicit_fk")
	@Column( name="explicit_type" )
	@AnyDiscriminatorValue( discriminator = "CARD", entity = CardPayment.class )
	@AnyDiscriminatorValue( discriminator = "CHECK", entity = CheckPayment.class )
	public Payment paymentExplicit;
	//end::associations-any-explicit-discriminator-example[]

	protected Order() {
		// for Hibernate use
	}

	public Order(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.mixed;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.orm.test.any.discriminator.CardPayment;
import org.hibernate.orm.test.any.discriminator.CheckPayment;
import org.hibernate.orm.test.any.discriminator.Payment;

import static org.hibernate.annotations.AnyDiscriminatorImplicitValues.Strategy.FULL_NAME;
import static org.hibernate.annotations.AnyDiscriminatorImplicitValues.Strategy.SHORT_NAME;

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

	//tag::associations-any-mixed-discriminator-full-example[]
	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "full_mixed_fk")
	@Column(name = "full_mixed_type")
	@AnyDiscriminatorImplicitValues(FULL_NAME)
	@AnyDiscriminatorValue( discriminator = "CARD", entity = CardPayment.class )
	@AnyDiscriminatorValue( discriminator = "CHECK", entity = CheckPayment.class )
	public Payment paymentMixedFullName;
	//end::associations-any-mixed-discriminator-full-example[]

	//tag::associations-any-mixed-discriminator-short-example[]
	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "short_mixed_fk")
	@Column(name = "short_mixed_type")
	@AnyDiscriminatorImplicitValues(SHORT_NAME)
	@AnyDiscriminatorValue( discriminator = "CARD", entity = CardPayment.class )
	@AnyDiscriminatorValue( discriminator = "CHECK", entity = CheckPayment.class )
	public Payment paymentMixedShortName;
	//end::associations-any-mixed-discriminator-short-example[]

	protected Order() {
		// for Hibernate use
	}

	public Order(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.implicit;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyKeyJavaClass;
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

	//tag::associations-any-implicit-discriminator-example[]
	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "implicit_fk")
	@Column(name = "implicit_type")
	public Payment paymentImplicit;
	//end::associations-any-implicit-discriminator-example[]

	//tag::associations-any-implicit-discriminator-full-example[]
	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "implicit_full_fk")
	@Column(name = "implicit_full_type")
	@AnyDiscriminatorImplicitValues(FULL_NAME)
	public Payment paymentImplicitFullName;
	//end::associations-any-implicit-discriminator-full-example[]

	//tag::associations-any-implicit-discriminator-short-example[]
	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "implicit_short_fk")
	@Column(name = "implicit_short_type")
	@AnyDiscriminatorImplicitValues(SHORT_NAME)
	public Payment paymentImplicitShortName;
	//end::associations-any-implicit-discriminator-short-example[]

	protected Order() {
		// for Hibernate use
	}

	public Order(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}

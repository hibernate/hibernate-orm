/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.mixed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.type.AnyDiscriminatorValueStrategy;

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

	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "explicit_fk")
	@AnyDiscriminatorValue( discriminator = "CARD", entity = CardPayment.class )
	@AnyDiscriminatorValue( discriminator = "CHECK", entity = CheckPayment.class )
	public Payment explicitPayment;

	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "implicit_fk")
	public Payment implicitPayment;

	@Any
	@AnyKeyJavaClass( Integer.class )
	@JoinColumn(name = "mixed_fk")
	@AnyDiscriminator(valueStrategy = AnyDiscriminatorValueStrategy.MIXED)
	@AnyDiscriminatorValue( discriminator = "CARD", entity = CardPayment.class )
	@AnyDiscriminatorValue( discriminator = "CHECK", entity = CheckPayment.class )
	public Payment mixedPayment;

	protected Order() {
		// for Hibernate use
	}

	public Order(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
}

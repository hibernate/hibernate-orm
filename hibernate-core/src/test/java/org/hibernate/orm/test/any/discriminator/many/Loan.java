/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.many;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.orm.test.any.discriminator.CardPayment;
import org.hibernate.orm.test.any.discriminator.CheckPayment;
import org.hibernate.orm.test.any.discriminator.Payment;

import java.util.Set;

import static org.hibernate.annotations.AnyDiscriminatorImplicitValues.Strategy.SHORT_NAME;

/**
 * @author Steve Ebersole
 */
@Entity
public class Loan {
	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::associations-many-to-any-example[]
	@ManyToAny
	@AnyDiscriminator(DiscriminatorType.STRING)
	@Column(name = "payment_type")
	@AnyKeyJavaClass(Integer.class)
	@AnyDiscriminatorValue( discriminator = "CARD", entity = CardPayment.class )
	@AnyDiscriminatorValue( discriminator = "CHECK", entity = CheckPayment.class )
	@AnyDiscriminatorImplicitValues(SHORT_NAME)
	@JoinTable(name = "loan_payments",
			joinColumns = @JoinColumn(name = "loan_fk"),
			inverseJoinColumns = @JoinColumn(name = "payment_fk")
	)
	private Set<Payment> payments;
	//end::associations-many-to-any-example[]

	protected Loan() {
		// for Hibernate use
	}

	public Loan(Integer id, String name) {
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

	public Set<Payment> getPayments() {
		return payments;
	}

	public void setPayments(Set<Payment> payments) {
		this.payments = payments;
	}
}

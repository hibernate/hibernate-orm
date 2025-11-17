/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import jakarta.persistence.MappedSuperclass;

/**
 * Represents a leg of a vanilla interest rate swap.
 */
@MappedSuperclass
public class Leg {
	/**
	 * Possible values of the payment frequency field.
	 */
	public enum Frequency {
		ANNUALY, SEMIANNUALLY, QUARTERLY, MONTHLY }

	;

	/**
	 * Shows how frequent payments according to this leg should be made.
	 */
	private Frequency paymentFrequency;

	public Frequency getPaymentFrequency() {
		return paymentFrequency;
	}

	public void setPaymentFrequency(Frequency paymentFrequency) {
		this.paymentFrequency = paymentFrequency;
	}

}

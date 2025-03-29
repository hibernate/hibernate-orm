/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


public class D {
	private Long id;
	private float amount;
	private A reverse;
	public A inverse;

	public D() {
		// try to induce an infinite loop in the lazy-loading machinery
		setAmount(100.0f);
		getAmount();
	}

	public D(Long id) {
		this();
		this.id = id;
	}

	/**
	 * Returns the amount.
	 * @return float
	 */
	public float getAmount() {
		return amount;
	}

	/**
	 * Returns the id.
	 * @return long
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the amount.
	 * @param amount The amount to set
	 */
	public void setAmount(float amount) {
		this.amount = amount;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	public A getReverse() {
		return reverse;
	}

	public void setReverse(A a) {
		reverse = a;
	}

}

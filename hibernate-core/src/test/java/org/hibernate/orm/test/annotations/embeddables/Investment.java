/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Chris Pheby
 */
@Embeddable
public class Investment {

	private DollarValue amount;
	private String description;
	@Column(name = "`date`")
	private MyDate date;

	public DollarValue getAmount() {
		return amount;
	}

	public void setAmount(DollarValue amount) {
		this.amount = amount;
	}

	@Column(length = 500)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public MyDate getDate() {
		return date;
	}

	public void setDate(MyDate date) {
		this.date = date;
	}
}

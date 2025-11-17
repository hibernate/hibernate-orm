/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.nested.fieldaccess;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Thomas Vanstals
 * @author Steve Ebersole
 */
@Embeddable
public class Investment {
	private MonetaryAmount amount;

	@Column(length = 500)
	private String description;
	private Date date;

	public MonetaryAmount getAmount() {
		return amount;
	}

	public void setAmount(MonetaryAmount amount) {
		this.amount = amount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}

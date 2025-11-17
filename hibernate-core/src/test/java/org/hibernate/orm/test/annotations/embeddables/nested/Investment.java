/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.nested;

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
	private String description;
	private Date date;

	public MonetaryAmount getAmount() {
		return amount;
	}

	public void setAmount(MonetaryAmount amount) {
		this.amount = amount;
	}

	@Column(length = 500)
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

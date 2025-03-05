/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

import java.sql.Date;

@Embeddable
public class ShelfLife implements java.io.Serializable {
	private Date inceptionDate;
	private Date soldDate;

	public ShelfLife() {
	}

	public ShelfLife(Date inceptionDate, Date soldDate) {
		this.inceptionDate = inceptionDate;
		this.soldDate = soldDate;
	}

	@Basic
	public Date getInceptionDate() {
		return inceptionDate;
	}

	public void setInceptionDate(Date inceptionDate) {
		this.inceptionDate = inceptionDate;
	}

	@Basic
	public Date getSoldDate() {
		return soldDate;
	}

	public void setSoldDate(Date soldDate) {
		this.soldDate = soldDate;
	}
}

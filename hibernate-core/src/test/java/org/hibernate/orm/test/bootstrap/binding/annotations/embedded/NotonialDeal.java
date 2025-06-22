/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@AttributeOverrides(value = {
		@AttributeOverride(name = "swap.tenor", column = @Column(name = "TENOR")), //should be ovvriden by deal
		@AttributeOverride(name = "id", column = @Column(name = "NOTONIALDEAL_ID"))
})
@MappedSuperclass
public class NotonialDeal extends Deal {
	/**
	 * Notional amount of both IRSs.
	 */
	private double notional;

	public double getNotional() {
		return notional;
	}

	public void setNotional(double notional) {
		this.notional = notional;
	}
}

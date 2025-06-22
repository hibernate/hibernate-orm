/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter.hbm;

//tag::basic-hbm-attribute-converter-mapping-money-example[]
public class Money {

	private long cents;

	public Money(long cents) {
		this.cents = cents;
	}

	public long getCents() {
		return cents;
	}

	public void setCents(long cents) {
		this.cents = cents;
	}
}
//end::basic-hbm-attribute-converter-mapping-money-example[]

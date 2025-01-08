/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates.model;

public class PojoRes {

	private final Decimal res;

	public PojoRes(Decimal res) {
		this.res = res;
	}

	public Decimal getRes() {
		return this.res;
	}

}

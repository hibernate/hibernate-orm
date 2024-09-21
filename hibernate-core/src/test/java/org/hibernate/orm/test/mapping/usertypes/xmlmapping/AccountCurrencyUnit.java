/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes.xmlmapping;

public class AccountCurrencyUnit implements CurrencyUnit {
	private String code;
	private int numericCode;

	public AccountCurrencyUnit() {
	}

	public AccountCurrencyUnit(String code, int numericCode) {
		this.code = code;
		this.numericCode = numericCode;
	}

	@Override
	public String getCurrencyCode() {
		return null;
	}

	@Override
	public int getNumericCode() {
		return 0;
	}

}

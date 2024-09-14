/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

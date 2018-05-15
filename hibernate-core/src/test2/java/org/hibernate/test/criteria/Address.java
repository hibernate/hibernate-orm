/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

/**
 * @author Gail Badner
 */
public class Address {
	private int addressId;

	public int getAddressId() {
		return addressId;
	}

	private String addressText;

	public String getAddressText() {
		return addressText;
	}

	public void setAddressText(String addressText) {
		this.addressText = addressText;
	}
}

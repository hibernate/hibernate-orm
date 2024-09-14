/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

	private String street;

	private String city;

	public Address() {
	}

	public Address(String street, String city) {
		this.street = street;
		this.city = city;
	}

	public String getCity() {
		return city;
	}

	public String getStreet() {
		return street;
	}

}

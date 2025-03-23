/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

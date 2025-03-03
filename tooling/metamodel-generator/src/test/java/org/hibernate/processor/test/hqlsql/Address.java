/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hqlsql;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public final class Address {
	private String street;
	private String city;
	private String zip;

	public Address(String street, String city, String zip) {
		this.street = street;
		this.city = city;
		this.zip = zip;
	}

	Address() {}

	public String street() {
		return street;
	}

	public String city() {
		return city;
	}

	public String zip() {
		return zip;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Address) obj;
		return Objects.equals(this.street, that.street) &&
				Objects.equals(this.city, that.city) &&
				Objects.equals(this.zip, that.zip);
	}

	@Override
	public int hashCode() {
		return Objects.hash(street, city, zip);
	}

	@Override
	public String toString() {
		return "Address[" +
				"street=" + street + ", " +
				"city=" + city + ", " +
				"zip=" + zip + ']';
	}
}

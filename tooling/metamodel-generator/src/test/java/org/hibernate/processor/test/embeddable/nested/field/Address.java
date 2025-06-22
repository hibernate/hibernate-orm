/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.nested.field;

import jakarta.persistence.Embeddable;
import org.hibernate.processor.test.embeddable.nested.property.Postcode;

import java.util.Objects;

@Embeddable
public final class Address {
	private String street;
	private String city;
	private org.hibernate.processor.test.embeddable.nested.property.Postcode postcode;


	public String getStreet() {
		return street;
	}

	public String getCity() {
		return city;
	}

	public Postcode getPostcode() {
		return postcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Address) obj;
		return Objects.equals(this.street, that.street) &&
				Objects.equals(this.city, that.city) &&
				Objects.equals(this.postcode, that.postcode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(street, city, postcode);
	}

	@Override
	public String toString() {
		return "Address[" +
				"street=" + street + ", " +
				"city=" + city + ", " +
				"postcode=" + postcode + ']';
	}
}

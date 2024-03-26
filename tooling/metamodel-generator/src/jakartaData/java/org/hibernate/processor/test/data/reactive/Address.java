package org.hibernate.processor.test.data.reactive;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public final class Address {
	private final String street;
	private final String city;
	private final String postcode;

	public Address(String street, String city, String postcode) {
		this.street = street;
		this.city = city;
		this.postcode = postcode;
	}

	public String street() {
		return street;
	}

	public String city() {
		return city;
	}

	public String postcode() {
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
}

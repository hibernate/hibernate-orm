package org.hibernate.orm.test.query.binding;

import jakarta.persistence.Embeddable;

@Embeddable
class Address {
	String street;
	String city;
	String state;

	Address() {}

	Address(String street, String city, String state) {
		this.street = street;
		this.city = city;
		this.state = state;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class Address implements Serializable {
	private String city;

	private String street;

	public Address() {
	}

	public Address(String city, String street) {
		this.city = city;
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}
}

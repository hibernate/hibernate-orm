/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.attr.transientattr;

public class EmbeddableWithTransient {
	private String street;
	private String city;
	private String fullAddress;
	private EmbeddableWithTransient component;

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getFullAddress() {
		return fullAddress;
	}

	public void setFullAddress(String fullAddress) {
		this.fullAddress = fullAddress;
	}

	public EmbeddableWithTransient getComponent() {
		return component;
	}

	public void setComponent(EmbeddableWithTransient component) {
		this.component = component;
	}
}

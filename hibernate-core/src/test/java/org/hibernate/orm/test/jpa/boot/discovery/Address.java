/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot.discovery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name="addresses")
public class Address {
	@Id
	private Integer id;
	private String street;
	private String city;
	private String state;
	private String zip;

	protected Address() {
		// for Hibernate use
	}

	public Integer getId() {
		return id;
	}

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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public Address(Integer id, String street, String city, String state, String zip) {

		this.id = id;
		this.street = street;
		this.city = city;
		this.state = state;
		this.zip = zip;
	}
}

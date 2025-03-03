/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile.mappedby;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.orm.test.annotations.fetchprofile.Customer6;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
@FetchProfile(name = "address-with-customer", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Address.class, association = "customer", mode = FetchMode.JOIN)
})
public class Address {

	@Id
	@GeneratedValue
	private long id;

	private String street;

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "address")
	private Customer6 customer;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Customer6 getCustomer() {
		return customer;
	}

	public void setCustomer(Customer6 customer) {
		this.customer = customer;
	}

}

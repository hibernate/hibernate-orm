/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.jointable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Christian Beikov
 */
@Entity(name="House")
public class Address {
	private Long id;
	private String street;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public String getStreet() {
		return street;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setStreet(String street) {
		this.street = street;
	}
}

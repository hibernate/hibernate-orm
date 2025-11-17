/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Farm {

	@Id
	private Integer id;

	@Basic
	private String name;

	@Embedded
	@Convert(
			attributeName = "city",
			converter = ToEntityAttributeThrowRuntimeExceptionConverter.class)
	private Address address;

	Farm() {
	}

	public Farm(Integer id, String name, Address address) {
		this.id = id;
		this.name = name;
		this.address = address;
	}

	public Integer getId() {
		return id;
	}

	public Address getAddress() {
		return address;
	}
}

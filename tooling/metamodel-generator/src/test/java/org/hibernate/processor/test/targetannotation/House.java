/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.targetannotation;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TargetEmbeddable;

/**
 * @author Hardy Ferentschik
 */
@Entity
class House {
	@Id
	long id;

	@Embedded
	@TargetEmbeddable(AddressImpl.class)
	private Address address;

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}

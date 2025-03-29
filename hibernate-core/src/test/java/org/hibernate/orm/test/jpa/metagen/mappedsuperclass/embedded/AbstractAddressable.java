/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.embedded;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public abstract class AbstractAddressable {
	private Address address;

	protected AbstractAddressable() {
	}

	protected AbstractAddressable(Address address) {
		this.address = address;
	}

	@Embedded
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}

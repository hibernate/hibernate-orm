/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.registered;

import jakarta.persistence.Column;

//tag::embeddable-instantiator-registration[]
public class Name {
	@Column(name = "first_name")
	private final String first;
	@Column(name = "last_name")
	private final String last;

	private Name() {
		throw new UnsupportedOperationException();
	}

	public Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	public String getFirstName() {
		return first;
	}

	public String getLastName() {
		return last;
	}
}
//end::embeddable-instantiator-registration[]

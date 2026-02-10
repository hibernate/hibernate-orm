/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddedid;

import jakarta.persistence.Embeddable;

@Embeddable
public class PersonId {
	private Long identifier;

	public PersonId() {
	}

	public PersonId(Long identifier) {
		this.identifier = identifier;
	}

	public Long getIdentifier() {
		return identifier;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddedid;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEntity<ID> {
	@EmbeddedId
	private ID id;

	private String name;

	public BaseEntity() {
	}

	public BaseEntity(ID id, String name) {
		this.id = id;
		this.name = name;
	}

	public ID getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}

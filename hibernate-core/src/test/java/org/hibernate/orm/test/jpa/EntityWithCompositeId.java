/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.io.Serializable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 *
 */
@Entity
public class EntityWithCompositeId implements Serializable {

	@EmbeddedId
	private CompositeId id;
	private String description;

	public CompositeId getId() {
		return id;
	}

	public void setId(CompositeId id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

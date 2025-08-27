/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.components.joins;

import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@jakarta.persistence.Entity
public class Entity {
	@Id
	@GeneratedValue
	private Long id;

	@Embedded
	private EmbeddedType embeddedType;

	public Entity() {
		// for jpa
	}

	public Entity(EmbeddedType embeddedType) {
		this.embeddedType = embeddedType;
	}

	public EmbeddedType getEmbeddedType() {
		return embeddedType;
	}

	public void setEmbeddedType(EmbeddedType embeddedType) {
		this.embeddedType = embeddedType;
	}
}

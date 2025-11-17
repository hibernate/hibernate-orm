/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.rfc9562;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.id.uuid.UuidVersion6Strategy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "entity_six")
@Entity
public class EntitySix {
	@Id
	@GeneratedValue
	@UuidGenerator(algorithm = UuidVersion6Strategy.class)
	private UUID id;
	@Basic
	private String name;

	protected EntitySix() {
		// for Hibernate use
	}

	public EntitySix(String name) {
		this.name = name;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

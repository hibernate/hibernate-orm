/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.annotation;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;

/**
 * @author Steve Ebersole
 */
@Entity
public class AnotherEntity {
	@Id
	@GeneratedValue
	@UuidGenerator( algorithm = CustomUuidValueGenerator.class )
	private UUID id;
	@Basic
	private String name;

	protected AnotherEntity() {
		// for Hibernate use
	}

	public AnotherEntity(String name) {
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

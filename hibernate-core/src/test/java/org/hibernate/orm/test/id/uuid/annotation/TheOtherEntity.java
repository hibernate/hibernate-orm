/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.annotation;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity(name = "TheOtherEntity")
@Table(name = "TheOtherEntity")
public class TheOtherEntity {
	@Id @GeneratedValue
	public Long pk;

	@UuidGenerator
	public UUID id;

	@Basic
	public String name;

	private TheOtherEntity() {
		// for Hibernate use
	}

	public TheOtherEntity(String name) {
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

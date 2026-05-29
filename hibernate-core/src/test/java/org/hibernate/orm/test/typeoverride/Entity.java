/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.typeoverride;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Gail Badner
 */
@jakarta.persistence.Entity
public class Entity {
	@Id
	@GeneratedValue
	private long id;
	private String name;

	public Entity() {
	}

	public Entity(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

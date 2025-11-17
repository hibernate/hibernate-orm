/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity with assigned identity
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Hotel {
	@Id
	private Long id;

	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

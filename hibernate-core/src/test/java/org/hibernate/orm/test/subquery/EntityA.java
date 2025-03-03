/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subquery;

import jakarta.persistence.*;

@Entity
public class EntityA {
	@Id
	@Column(name = "id", nullable = false)
	private int id;
	private String name;

	public EntityA() {
	}

	public EntityA(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

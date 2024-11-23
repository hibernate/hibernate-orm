/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Paul Cowan
 */
@Entity
public class Monkey {
	private String id;

	@Id
	@GeneratedValue(generator = "system-uuid-2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}

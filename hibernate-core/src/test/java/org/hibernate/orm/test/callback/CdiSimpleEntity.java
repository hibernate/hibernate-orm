/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = "CdiSimpleEntity")
public class CdiSimpleEntity {
	@Id
	@GeneratedValue
	Long id;

	String name;

	protected CdiSimpleEntity() {
	}

	public CdiSimpleEntity(String name) {
		this.name = name;
	}
}

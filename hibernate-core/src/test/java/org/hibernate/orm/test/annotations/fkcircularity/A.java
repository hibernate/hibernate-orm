/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fkcircularity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * Test entities ANN-722.
 *
 * @author Hardy Ferentschik
 *
 */
@Entity
public class A {
	private A_PK id;

	@EmbeddedId
	public A_PK getId() {
		return id;
	}

	public void setId(A_PK id) {
		this.id = id;
	}
}

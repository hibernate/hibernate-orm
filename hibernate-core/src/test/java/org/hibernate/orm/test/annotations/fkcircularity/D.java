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
public class D {
	private D_PK id;

	@EmbeddedId
	public D_PK getId() {
		return id;
	}

	public void setId(D_PK id) {
		this.id = id;
	}
}

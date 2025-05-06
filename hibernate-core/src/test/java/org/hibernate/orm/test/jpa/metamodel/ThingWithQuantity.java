/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.Entity;

/**
 * @author James Gilbertson
 */
@Entity
public class ThingWithQuantity extends Thing {
	private int quantity;

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}

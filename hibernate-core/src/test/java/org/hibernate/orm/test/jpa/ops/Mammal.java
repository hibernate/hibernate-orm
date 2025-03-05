/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mammal extends Animal {
	private int mamalNbr;

	public int getMamalNbr() {
		return mamalNbr;
	}

	public void setMamalNbr(int mamalNbr) {
		this.mamalNbr = mamalNbr;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * @author Gavin King
 *
 * @deprecated Use {@link org.hibernate.testing.orm.domain.animal.DomesticAnimal} instead
 */
@Deprecated
public class DomesticAnimal extends Mammal {
	private Human owner;

	public Human getOwner() {
		return owner;
	}

	public void setOwner(Human owner) {
		this.owner = owner;
	}
}

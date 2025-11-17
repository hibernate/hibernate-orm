/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Access;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Access(jakarta.persistence.AccessType.FIELD)
public class Hominidae extends Mammals {
	private int intelligence;

	public int getIntelligence() {
		return intelligence;
	}

	public void setIntelligence(int intelligence) {
		this.intelligence = intelligence;
	}

	public int getNonPersistent() {
		return 0;
	}
}

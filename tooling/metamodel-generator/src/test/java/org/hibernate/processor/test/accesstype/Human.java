/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Human extends Hominidae {
	private int nonPersistent;
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

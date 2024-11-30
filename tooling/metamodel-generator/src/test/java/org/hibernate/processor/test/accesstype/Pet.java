/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Pet {
	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNonPersistent() {
		return null;
	}
}

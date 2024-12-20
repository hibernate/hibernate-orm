/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;
import jakarta.persistence.Entity;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Horse extends Animal {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

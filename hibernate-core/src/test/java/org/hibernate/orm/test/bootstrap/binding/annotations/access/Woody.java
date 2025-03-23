/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;
import jakarta.persistence.AccessType;
import jakarta.persistence.MappedSuperclass;

import jakarta.persistence.Access;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
@Access(AccessType.PROPERTY)
public class Woody extends Thingy {
	private String color;
	private String name;
	public boolean isAlive; //shouldn't be persistent

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

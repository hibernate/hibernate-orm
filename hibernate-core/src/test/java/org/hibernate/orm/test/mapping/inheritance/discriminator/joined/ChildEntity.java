/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator.joined;

/**
 * @author Chris Cranford
 */
public class ChildEntity extends ParentEntity {
	private String name;

	ChildEntity() {

	}

	ChildEntity(Integer id, String name) {
		super( id );
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

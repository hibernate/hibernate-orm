/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization.entity;

/**
 * The class should be in a package that is different from the test
 * so that the test does not have access to private field,
 * and the protected getter and setter.
 *
 * @author Gail Badner
 */
public class AnEntity {
	private PK pk;

	public AnEntity() {
	}

	public AnEntity(PK pk) {
		this.pk = pk;
	}

	protected PK getPk() {
		return pk;
	}

	protected void setPk(PK pk) {
		this.pk = pk;
	}
}

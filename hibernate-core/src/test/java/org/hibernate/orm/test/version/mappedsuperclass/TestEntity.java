/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.mappedsuperclass;

import jakarta.persistence.Entity;

/**
 * @author Andrea Boriero
 */
@Entity
public class TestEntity extends AbstractEntity {
	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

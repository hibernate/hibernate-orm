/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.multilevelsuperclass;

/**
 * Grandparent of the hierarchy. Declares the mapped {@code name} property so that,
 * with property access, its getter is inherited by both {@link MultiLevelMiddle}
 * and {@link MultiLevelChild}.
 */
public class MultiLevelBase {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

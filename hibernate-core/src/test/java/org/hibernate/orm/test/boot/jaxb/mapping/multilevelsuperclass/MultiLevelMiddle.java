/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.multilevelsuperclass;

/**
 * Intermediate (unmapped) superclass. Declares only the mapped {@code id} property;
 * the {@code name} getter is inherited from {@link MultiLevelBase}.
 */
public class MultiLevelMiddle extends MultiLevelBase {
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}

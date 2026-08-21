/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.multilevelsuperclass;

/**
 * The mapped entity. Declares its own {@code data} property while inheriting the
 * mapped {@code id} (from {@link MultiLevelMiddle}) and {@code name}
 * (from {@link MultiLevelBase}).
 */
public class MultiLevelChild extends MultiLevelMiddle {
	private String data;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}

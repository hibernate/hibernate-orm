/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.recursivecomponent;

public class MyEntity {
	private Long id;
	private MyComponent component;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MyComponent getComponent() {
		return component;
	}

	public void setComponent(MyComponent component) {
		this.component = component;
	}
}

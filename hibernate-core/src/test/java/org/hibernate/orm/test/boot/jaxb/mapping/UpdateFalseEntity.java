/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

public class UpdateFalseEntity {
	private Long id;
	private EmbeddableComponent component;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EmbeddableComponent getComponent() {
		return component;
	}

	public void setComponent(EmbeddableComponent component) {
		this.component = component;
	}
}

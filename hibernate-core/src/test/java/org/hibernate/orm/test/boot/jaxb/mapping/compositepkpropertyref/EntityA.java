/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.compositepkpropertyref;

public class EntityA {
	private Long id;
	private String name;
	private Object entityB;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getEntityB() {
		return entityB;
	}

	public void setEntityB(Object entityB) {
		this.entityB = entityB;
	}
}

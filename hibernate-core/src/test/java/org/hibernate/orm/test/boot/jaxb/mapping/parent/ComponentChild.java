/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.parent;

public class ComponentChild {
	private ParentEntity owner;
	private String description;

	public ParentEntity getOwner() {
		return owner;
	}

	public void setOwner(ParentEntity owner) {
		this.owner = owner;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

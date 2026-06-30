/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.parent;

public class CompositeChild {
	private ParentEntity parent;
	private String name;
	private NestedAddress address;

	public ParentEntity getParent() {
		return parent;
	}

	public void setParent(ParentEntity parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NestedAddress getAddress() {
		return address;
	}

	public void setAddress(NestedAddress address) {
		this.address = address;
	}
}

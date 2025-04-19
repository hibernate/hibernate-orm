/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer.child;

import org.hibernate.orm.test.bytecode.enhancement.optimizer.ParentEntity;

import jakarta.persistence.Entity;

@Entity(name = "ChildEntity")
public class ChildEntity extends ParentEntity {
	private String childField;

	public String getChildField() {
		return childField;
	}

	public void setChieldField(String childField) {
		this.childField = childField;
	}
}

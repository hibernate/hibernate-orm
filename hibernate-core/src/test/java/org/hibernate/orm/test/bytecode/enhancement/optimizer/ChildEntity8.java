/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import jakarta.persistence.Entity;

@Entity(name = "ChildEntity8")
public class ChildEntity8 extends AncestorEntity {
	private String childField;

	public String getChildField() {
		return childField;
	}

	public void setChildField(String childField) {
		this.childField = childField;
	}
}

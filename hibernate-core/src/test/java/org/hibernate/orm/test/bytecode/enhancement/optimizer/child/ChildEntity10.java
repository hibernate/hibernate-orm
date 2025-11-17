/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer.child;

import jakarta.persistence.Entity;
import org.hibernate.orm.test.bytecode.enhancement.optimizer.AncestorEntity;

@Entity(name = "ChildEntity10")
public class ChildEntity10 extends AncestorEntity {
	private String childField;

	public String getChildField() {
		return childField;
	}

	public void setChieldField(String childField) {
		this.childField = childField;
	}
}

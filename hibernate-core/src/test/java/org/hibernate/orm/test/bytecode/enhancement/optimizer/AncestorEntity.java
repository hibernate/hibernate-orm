/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.orm.test.bytecode.enhancement.optimizer.parent.Ancestor;

@Entity(name = "AncestorEntity")
@Inheritance(strategy = InheritanceType.JOINED)
public class AncestorEntity extends Ancestor {

	private Long id;

	private String field;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}
}

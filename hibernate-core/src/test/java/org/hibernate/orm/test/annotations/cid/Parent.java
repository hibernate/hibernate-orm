/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * Entity with composite id
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Parent {
	@EmbeddedId
	public ParentPk id;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Parent ) ) return false;

		final Parent parent = (Parent) o;

		if ( !id.equals( parent.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}

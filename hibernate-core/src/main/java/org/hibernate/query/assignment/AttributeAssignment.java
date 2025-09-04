/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.assignment;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Assignment of a value to an attribute.
 *
 * @author Gavin King
 */
record AttributeAssignment<T, X>(SingularAttribute<T, X> attribute, X value)
		implements Assignment<T> {
	@Override
	public void apply(SqmUpdateStatement<? extends T> update) {
		update.set( attribute, value );
	}
}

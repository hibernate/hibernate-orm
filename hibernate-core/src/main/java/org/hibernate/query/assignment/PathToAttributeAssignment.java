/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.assignment;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Assignment of a path to an attribute.
 *
 * @author Gavin King
 */
record PathToAttributeAssignment<T, X>(SingularAttribute<T, X> attribute, Path<T,X> value)
		implements Assignment<T> {
	@Override
	public void apply(SqmUpdateStatement<? extends T> update) {
		update.set( attribute, value.path( update.getRoot() ) );
	}
}

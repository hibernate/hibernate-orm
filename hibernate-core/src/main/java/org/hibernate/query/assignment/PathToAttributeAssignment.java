/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.assignment;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 * Assignment of a path to an attribute.
 *
 * @author Gavin King
 */
record PathToAttributeAssignment<T, X>(@Nonnull SingularAttribute<T, X> attribute, @Nonnull Path<T,X> value)
		implements Assignment<T> {
	@Override
	public void apply(@Nonnull SqmUpdateStatement<? extends T> update) {
		update.set( attribute, value.path( update.getRoot() ) );
	}
}

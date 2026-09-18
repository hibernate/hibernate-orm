/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.assignment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 * Assignment of a value to a path.
 *
 * @author Gavin King
 */
record PathAssignment<T, X>(@Nonnull Path<T, X> path, @Nullable X value)
		implements Assignment<T> {
	@Override
	public void apply(@Nonnull SqmUpdateStatement<? extends T> update) {
		update.set( path.path( update.getRoot() ), value );
	}
}

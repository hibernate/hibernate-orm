package org.hibernate.query.assignment;

import jakarta.annotation.Nonnull;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 *  * Assignment of a path to a path.
 *
 * @author Gavin King
 */
record PathToPathAssignment<T, X>(@Nonnull Path<T, X> path, @Nonnull Path<T,X> value)
		implements Assignment<T> {
	@Override
	public void apply(@Nonnull SqmUpdateStatement<? extends T> update) {
		update.set( path.path( update.getRoot() ), value.path( update.getRoot() ) );
	}
}

package org.hibernate.query.assignment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 * Assignment of a value to an attribute.
 *
 * @author Gavin King
 */
record AttributeAssignment<T, X>(@Nonnull SingularAttribute<T, X> attribute, @Nullable X value)
		implements Assignment<T> {
	@Override
	public void apply(@Nonnull SqmUpdateStatement<? extends T> update) {
		update.set( attribute, value );
	}
}

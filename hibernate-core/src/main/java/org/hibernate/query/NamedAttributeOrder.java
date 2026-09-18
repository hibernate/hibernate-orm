/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * An {@link Order} based on the name of an attribute of an entity.
 *
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record NamedAttributeOrder<X>
		(@Nonnull SortDirection direction, @Nonnull Nulls nullPrecedence, @Nonnull Class<X> entityClass, @Nonnull String attributeName, boolean caseSensitive)
		implements Order<X> {

	NamedAttributeOrder(@Nonnull SortDirection order, @Nonnull Nulls nullPrecedence, @Nonnull Class<X> entityClass, @Nonnull String attributeName) {
		this( order, nullPrecedence, entityClass, attributeName, true );
	}

	private NamedAttributeOrder(@Nonnull NamedAttributeOrder<X> that, @Nonnull Nulls nullPrecedence) {
		this( that.direction, nullPrecedence, that.entityClass, that.attributeName, that.caseSensitive );
	}

	private NamedAttributeOrder(@Nonnull NamedAttributeOrder<X> that, @Nonnull SortDirection direction) {
		this( direction, that.nullPrecedence, that.entityClass, that.attributeName, that.caseSensitive );
	}

	private NamedAttributeOrder(@Nonnull NamedAttributeOrder<X> that, boolean caseSensitive) {
		this( that.direction, that.nullPrecedence, that.entityClass, that.attributeName, caseSensitive );
	}

	@Override
	public int element() {
		return 1;
	}

	@Override
	@Nullable
	public SingularAttribute<X, ?> attribute() {
		return null;
	}

	@Override
	@Nonnull
	public Order<X> ignoringCase() {
		return new NamedAttributeOrder<>( this, false );
	}

	@Override
	@Nonnull
	public Order<X> reverse() {
		return new NamedAttributeOrder<>( this, direction.reverse() );
	}

	@Override
	@Nonnull
	public Order<X> withNullsFirst() {
		return new NamedAttributeOrder<>( this, Nulls.FIRST );
	}

	@Override
	@Nonnull
	public Order<X> withNullsLast() {
		return new NamedAttributeOrder<>( this, Nulls.LAST );
	}

	@Override
	@Nonnull
	public String toString() {
		return attributeName + " " + direction;
	}

}

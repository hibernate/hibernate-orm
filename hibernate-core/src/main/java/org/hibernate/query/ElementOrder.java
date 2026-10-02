package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * An {@link Order} based on an indexed element of a select clause.
 *
 * @param <X> The query result type
 *
 * @author Gavin King
 */
record ElementOrder<X>
		(@Nonnull SortDirection direction, @Nonnull Nulls nullPrecedence, int element, boolean caseSensitive)
		implements Order<X> {

	ElementOrder(@Nonnull SortDirection order, @Nonnull Nulls nullPrecedence, int element) {
		this( order, nullPrecedence, element, true );
	}

	@Override
	@Nullable
	public Class<X> entityClass() {
		return null;
	}

	@Override
	@Nullable
	public String attributeName() {
		return null;
	}

	@Override
	@Nullable
	public SingularAttribute<X, ?> attribute() {
		return null;
	}

	@Override
	@Nonnull
	public Order<X> ignoringCase() {
		return new ElementOrder<>( direction, nullPrecedence, element, false );
	}

	@Override
	@Nonnull
	public Order<X> reverse() {
		return new ElementOrder<>( direction.reverse(), nullPrecedence, element, caseSensitive );
	}

	@Override
	@Nonnull
	public Order<X> withNullsFirst() {
		return new ElementOrder<>( direction, Nulls.FIRST, element );
	}

	@Override
	@Nonnull
	public Order<X> withNullsLast() {
		return new ElementOrder<>( direction, Nulls.LAST, element );
	}

	@Override
	@Nonnull
	public String toString() {
		return element + " " + direction;
	}

}

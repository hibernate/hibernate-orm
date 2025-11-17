/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

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
		(SortDirection direction, Nulls nullPrecedence, int element, boolean caseSensitive)
		implements Order<X> {

	ElementOrder(SortDirection order, Nulls nullPrecedence, int element) {
		this( order, nullPrecedence, element, true );
	}

	@Override
	public Class<X> entityClass() {
		return null;
	}

	@Override
	public String attributeName() {
		return null;
	}

	@Override
	public SingularAttribute<X, ?> attribute() {
		return null;
	}

	@Override
	public Order<X> ignoringCase() {
		return new ElementOrder<>( direction, nullPrecedence, element, false );
	}

	@Override
	public Order<X> reverse() {
		return new ElementOrder<>( direction.reverse(), nullPrecedence, element, caseSensitive );
	}

	@Override
	public Order<X> withNullsFirst() {
		return new ElementOrder<>( direction, Nulls.FIRST, element );
	}

	@Override
	public Order<X> withNullsLast() {
		return new ElementOrder<>( direction, Nulls.LAST, element );
	}

	@Override
	public String toString() {
		return element + " " + direction;
	}

}

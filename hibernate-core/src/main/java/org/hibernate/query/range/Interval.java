/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * Restricts to an upper-bounded and lower-bounded interval.
 */
record Interval<U extends Comparable<U>>(LowerBound<U> lowerBound, UpperBound<U> upperBound)
		implements Range<U> {
	@Override
	public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder) {
		return lowerBound.open() || upperBound.open()
				? builder.and( lowerBound.toPredicate( root, attribute, builder ),
				upperBound.toPredicate( root, attribute, builder ) )
				: builder.between( root.get( attribute ),
						builder.literal( lowerBound.bound() ), builder.literal( upperBound.bound() ) );
	}
}

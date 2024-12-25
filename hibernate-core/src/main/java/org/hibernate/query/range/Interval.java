/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Restricts to an upper-bounded and lower-bounded interval.
 */
record Interval<U extends Comparable<U>>(LowerBound<U> lowerBound, UpperBound<U> upperBound)
		implements Range<U> {
	@Override
	public Predicate toPredicate(Path<U> path, CriteriaBuilder builder) {
		return lowerBound.open() || upperBound.open()
				? builder.and( lowerBound.toPredicate( path, builder ), upperBound.toPredicate( path, builder ) )
				: builder.between( path, builder.literal( lowerBound.bound() ), builder.literal( upperBound.bound() ) );
	}

	@Override
	public Class<? extends U> getType() {
		return lowerBound.getType();
	}
}

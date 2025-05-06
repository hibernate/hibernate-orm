/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * An upper-bounded and lower-bounded interval.
 *
 * @author Gavin King
 */
record Interval<U extends Comparable<U>>(LowerBound<U> lowerBound, UpperBound<U> upperBound)
		implements Range<U> {
	@Override
	public Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder) {
		return lowerBound.open() || upperBound.open()
				? builder.and( lowerBound.toPredicate( path, builder ), upperBound.toPredicate( path, builder ) )
				: builder.between( path, lowerBound.bound(), upperBound.bound() );
	}

	@Override
	public Class<? extends U> getType() {
		return lowerBound.getType();
	}
}

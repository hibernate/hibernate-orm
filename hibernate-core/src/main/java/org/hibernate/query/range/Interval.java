/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * An upper-bounded and lower-bounded interval.
 *
 * @author Gavin King
 */
record Interval<U extends Comparable<U>>(@Nonnull LowerBound<U> lowerBound, @Nonnull UpperBound<U> upperBound)
		implements Range<U> {
	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Path<? extends U> path, @Nonnull CriteriaBuilder builder) {
		return lowerBound.open() || upperBound.open()
				? builder.and( lowerBound.toPredicate( path, builder ), upperBound.toPredicate( path, builder ) )
				: builder.between( path, lowerBound.bound(), upperBound.bound() );
	}

	@Nonnull
	@Override
	public Class<? extends U> getType() {
		return lowerBound.getType();
	}
}

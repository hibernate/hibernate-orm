/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * The {@link Range} of all values below a given upper bound.
 *
 * @author Gavin King
 */
record UpperBound<U extends Comparable<U>>(U bound, boolean open) implements Range<U> {
	UpperBound {
		Objects.requireNonNull( bound, "bound is null" );
	}

	@Override
	public Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder) {
		return open
				? builder.lessThan( path, bound )
				: builder.lessThanOrEqualTo( path, bound );
	}

	@Override @SuppressWarnings("unchecked")
	public Class<? extends U> getType() {
		return (Class<? extends U>) bound.getClass();
	}
}

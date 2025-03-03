/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * The {@link Range} of all values above a given lower bound.
 *
 * @author Gavin King
 */
record LowerBound<U extends Comparable<U>>(U bound, boolean open) implements Range<U> {
	LowerBound {
		Objects.requireNonNull( bound, "bound is null" );
	}

	@Override
	public Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder) {
		// TODO: it would be much better to not do use literal,
		//       and let it be treated as a parameter, but we
		//       we run into the usual bug with parameters in
		//       manipulated SQM trees
		final Expression<U> literal = builder.literal( bound );
		return open
				? builder.greaterThan( path, literal )
				: builder.greaterThanOrEqualTo( path, literal );
	}

	@Override @SuppressWarnings("unchecked")
	public Class<? extends U> getType() {
		return (Class<? extends U>) bound.getClass();
	}
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
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
		// TODO: it would be much better to not do use literal,
		//       and let it be treated as a parameter, but we
		//       we run into the usual bug with parameters in
		//       manipulated SQM trees
		final Expression<U> literal = builder.literal( bound );
		return open
				? builder.lessThan( path, literal )
				: builder.lessThanOrEqualTo( path, literal );
	}

	@Override @SuppressWarnings("unchecked")
	public Class<? extends U> getType() {
		return (Class<? extends U>) bound.getClass();
	}
}

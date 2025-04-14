/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.internal.util.ReflectHelper;

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
		return open
				? builder.greaterThan( path, bound )
				: builder.greaterThanOrEqualTo( path, bound );
	}

	@Override
	public Class<? extends U> getType() {
		return ReflectHelper.getClass( bound );
	}
}

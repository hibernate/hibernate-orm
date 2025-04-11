/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.internal.util.ReflectHelper;

import java.util.Objects;

/**
 * A {@link Range} with a single literal value.
 *
 * @author Gavin King
 */
record Value<U>(U value) implements Range<U> {
	Value {
		Objects.requireNonNull( value, "value is null" );
	}

	@Override
	public Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder) {
		// TODO: it would be much better to not do use literal,
		//       and let it be treated as a parameter, but we
		//       we run into the usual bug with parameters in
		//       manipulated SQM trees
		final Expression<U> literal = builder.literal( value );
		return path.equalTo( literal );
	}

	@Override
	public Class<? extends U> getType() {
		return ReflectHelper.getClass( value.getClass() );
	}
}

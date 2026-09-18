/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.internal.util.ReflectHelper;

import java.util.Objects;

/**
 * A {@link Range} with a single literal value.
 *
 * @author Gavin King
 */
record Value<U>(@Nonnull U value) implements Range<U> {
	Value {
		Objects.requireNonNull( value, "value is null" );
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Path<? extends U> path, @Nonnull CriteriaBuilder builder) {
		return path.equalTo( value );
	}

	@Nonnull
	@Override
	public Class<? extends U> getType() {
		return ReflectHelper.getClass( value.getClass() );
	}
}

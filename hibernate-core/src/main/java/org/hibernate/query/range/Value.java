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
		return path.equalTo( value );
	}

	@Override
	public Class<? extends U> getType() {
		return ReflectHelper.getClass( value.getClass() );
	}
}

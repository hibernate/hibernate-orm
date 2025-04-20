/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.internal.util.ReflectHelper;

import java.util.List;
import java.util.Objects;

/**
 * Restricts to a list of literal values.
 *
 * @author Gavin King
 */
record ValueList<U>(List<U> values) implements Range<U> {
	ValueList {
		Objects.requireNonNull( values, "value list is null" );
		if ( values.isEmpty() ) {
			throw new IllegalArgumentException( "value list is empty" );
		}
	}

	@Override
	public Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder) {
		return path.in( values );
	}

	@Override
	public Class<? extends U> getType() {
		return ReflectHelper.getClass( values.get(0) );
	}
}

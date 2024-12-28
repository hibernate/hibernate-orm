/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;
import java.util.Objects;

/**
 * A {@link Range} with a single literal string, ignoring case.
 *
 * @author Gavin King
 */
record CaseInsensitiveValue(String value) implements Range<String> {
	CaseInsensitiveValue {
		Objects.requireNonNull( value, "value is null" );
	}

	@Override
	public Predicate toPredicate(Path<String> path, CriteriaBuilder builder) {
		// TODO: it would be much better to not do use literal,
		//       and let it be treated as a parameter, but we
		//       we run into the usual bug with parameters in
		//       manipulated SQM trees
		final Expression<String> literal = builder.literal( value.toLowerCase( Locale.ROOT ) );
		return builder.lower( path ).equalTo( literal );
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}
}

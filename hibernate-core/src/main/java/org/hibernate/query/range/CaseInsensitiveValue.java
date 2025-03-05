/*
 * SPDX-License-Identifier: Apache-2.0
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
	public Predicate toPredicate(Path<? extends String> path, CriteriaBuilder builder) {
		// TODO: it would be much better to not do use literal,
		//       and let it be treated as a parameter, but we
		//       we run into the usual bug with parameters in
		//       manipulated SQM trees
		@SuppressWarnings("unchecked")
		final Path<String> stringPath = (Path<String>) path; // safe, because String is final
		final Expression<String> literal = builder.literal( value.toLowerCase( Locale.ROOT ) );
		return builder.lower( stringPath ).equalTo( literal );
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;
import java.util.Objects;

/**
 * A {@link Range} with a single literal string, ignoring case.
 *
 * @author Gavin King
 */
record CaseInsensitiveValue(@Nonnull String value) implements Range<String> {
	CaseInsensitiveValue {
		Objects.requireNonNull( value, "value is null" );
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Path<? extends String> path, @Nonnull CriteriaBuilder builder) {
		@SuppressWarnings("unchecked")
		final Path<String> stringPath = (Path<String>) path; // safe, because String is final
		return builder.lower( stringPath ).equalTo( value.toLowerCase( Locale.ROOT ) );
	}

	@Nonnull
	@Override
	public Class<String> getType() {
		return String.class;
	}
}

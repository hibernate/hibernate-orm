/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * Restricts a string by a pattern.
 */
record Pattern(String pattern, boolean caseSensitive) implements Range<String> {
	@Override
	public Predicate toPredicate(Path<String> path, CriteriaBuilder builder) {
		return caseSensitive
				? builder.like( path, builder.literal( pattern ) )
				: builder.like( builder.lower( path ),
						builder.literal( pattern.toLowerCase( Locale.ROOT ) ) );
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}
}

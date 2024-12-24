/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.Locale;

/**
 * Restricts a string by a pattern.
 */
record Pattern(String pattern, boolean caseSensitive) implements Range<String> {
	@Override
	public <X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, String> attribute, CriteriaBuilder builder) {
		return caseSensitive
				? builder.like( root.get( attribute ), builder.literal( pattern ) )
				: builder.like( builder.lower( root.get( attribute ) ),
						builder.literal( pattern.toLowerCase( Locale.ROOT ) ) );
	}
}

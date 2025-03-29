/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * The {@link Range} of strings recognized by the given pattern.
 *
 * @author Gavin King
 */
record Pattern(String pattern, boolean caseSensitive) implements Range<String> {
	Pattern(String pattern, boolean caseSensitive, char charWildcard, char stringWildcard) {
		this( translate( pattern, charWildcard, stringWildcard ), caseSensitive );
	}

	@Override
	public Predicate toPredicate(Path<? extends String> path, CriteriaBuilder builder) {
		@SuppressWarnings("unchecked")
		final Path<String> stringPath = (Path<String>) path; // safe, because String is final
		return caseSensitive
				? builder.like( stringPath, builder.literal( pattern ), '\\' )
				: builder.like( builder.lower( stringPath ),
						builder.literal( pattern.toLowerCase( Locale.ROOT ) ),
						'\\' );
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}

	private static String translate(String pattern, char charWildcard, char stringWildcard) {
		final var result = new StringBuilder();
		for ( int i = 0; i < pattern.length(); i++ ) {
			final char ch = pattern.charAt( i );
			if ( ch == charWildcard ) {
				result.append( '_' );
			}
			else if ( ch == stringWildcard ) {
				result.append( '%' );
			}
			else {
				if ( ch == '%' || ch == '_' || ch == '\\' ) {
					result.append( '\\' );
				}
				result.append( ch );
			}
		}
		return result.toString();
	}
}

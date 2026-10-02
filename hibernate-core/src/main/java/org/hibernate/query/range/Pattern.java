package org.hibernate.query.range;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * The {@link Range} of strings recognized by the given pattern.
 *
 * @author Gavin King
 */
record Pattern(@Nonnull String pattern, boolean caseSensitive) implements Range<String> {

	// default escape and wildcard characters
	private static final char ESCAPE = '\\';
	private static final char WILDCARD_CHAR = '_';
	private static final char WILDCARD_STRING = '%';

	Pattern(@Nonnull String pattern, boolean caseSensitive, char charWildcard, char stringWildcard) {
		this( translate( pattern, charWildcard, stringWildcard ), caseSensitive );
	}

	@Nonnull
	@Override
	public Predicate toPredicate(@Nonnull Path<? extends String> path, @Nonnull CriteriaBuilder builder) {
		@SuppressWarnings("unchecked")
		final Path<String> stringPath = (Path<String>) path; // safe, because String is final
		return caseSensitive
				? builder.like( stringPath, pattern, ESCAPE )
				: builder.like( builder.lower( stringPath ), pattern.toLowerCase( Locale.ROOT ), ESCAPE );
	}

	@Nonnull
	@Override
	public Class<String> getType() {
		return String.class;
	}

	@Nonnull
	private static String translate(@Nonnull String pattern, char charWildcard, char stringWildcard) {
		final var result = new StringBuilder();
		for ( int i = 0; i < pattern.length(); i++ ) {
			final char ch = pattern.charAt( i );
			if ( ch == charWildcard ) {
				result.append( WILDCARD_CHAR );
			}
			else if ( ch == stringWildcard ) {
				result.append( WILDCARD_STRING );
			}
			else {
				if ( ch == WILDCARD_STRING || ch == WILDCARD_CHAR || ch == ESCAPE ) {
					result.append( ESCAPE );
				}
				result.append( ch );
			}
		}
		return result.toString();
	}
}

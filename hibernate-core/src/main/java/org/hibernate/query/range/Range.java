/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.query.Restriction;

import java.util.List;

/**
 * Specifies an allowed set of range of values for a value being restricted.
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} may be declared with type {@code Range<T>} where {@code T}
 * is the type of the matching field of property of the entity.
 *
 * @param <U> The type of the value being restricted
 *
 * @see Restriction
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating
public interface Range<U> {

	Class<? extends U> getType();

	/**
	 * Return a JPA Criteria {@link Predicate} constraining the given
	 * attribute of the given root entity to this domain of allowed
	 * values.
	 */
	@Internal
	Predicate toPredicate(Path<? extends U> path, CriteriaBuilder builder);

	static <U> Range<U> singleValue(U value) {
		return new Value<>( value );
	}

	static Range<String> singleCaseInsensitiveValue(String value) {
		return new CaseInsensitiveValue( value );
	}

	static <U> Range<U> valueList(List<U> values) {
		return new ValueList<>( values );
	}

	static <U extends Comparable<U>> Range<U> greaterThan(U bound) {
		return new LowerBound<>( bound, true );
	}

	static <U extends Comparable<U>> Range<U> greaterThanOrEqualTo(U bound) {
		return new LowerBound<>( bound, false );
	}

	static <U extends Comparable<U>> Range<U> lessThan(U bound) {
		return new UpperBound<>( bound, true );
	}

	static <U extends Comparable<U>> Range<U> lessThanOrEqualTo(U bound) {
		return new UpperBound<>( bound, false );
	}

	static <U extends Comparable<U>> Range<U> open(U lowerBound, U upperBound) {
		return new Interval<>( new LowerBound<>( lowerBound, true ),
				new UpperBound<>( upperBound, true ) );
	}

	static <U extends Comparable<U>> Range<U> closed(U lowerBound, U upperBound) {
		return new Interval<>( new LowerBound<>( lowerBound, false ),
				new UpperBound<>( upperBound, false ) );
	}

	static Range<String> pattern(String pattern, boolean caseSensitive) {
		return new Pattern( pattern, caseSensitive );
	}

	static Range<String> pattern(String pattern, boolean caseSensitive, char charWildcard, char stringWildcard) {
		return new Pattern( pattern, caseSensitive, charWildcard, stringWildcard );
	}

	static Range<String> pattern(String pattern) {
		return pattern( pattern, true );
	}

	static Range<String> prefix(String prefix, boolean caseSensitive) {
		return pattern( escape( prefix ) + '%', caseSensitive );
	}

	static Range<String> suffix(String suffix, boolean caseSensitive) {
		return pattern( '%' + escape( suffix ), caseSensitive );
	}

	static Range<String> containing(String substring, boolean caseSensitive) {
		return pattern( '%' + escape( substring ) + '%', caseSensitive );
	}

	static Range<String> prefix(String prefix) {
		return prefix( prefix, true );
	}

	static Range<String> suffix(String suffix) {
		return pattern( suffix, true );
	}

	static Range<String> containing(String substring) {
		return pattern( substring, true );
	}

	static <U> Range<U> empty(Class<U> type) {
		return new EmptyRange<>( type );
	}

	static <U> Range<U> full(Class<U> type) {
		return new FullRange<>( type );
	}

	static <U> Range<U> notNull(Class<U> type) {
		return new NotNull<>( type );
	}

	private static String escape(String literal) {
		final var result = new StringBuilder();
		for ( int i = 0; i < literal.length(); i++ ) {
			final char ch = literal.charAt( i );
			if ( ch=='%' || ch=='_' || ch=='\\' ) {
				result.append('\\');
			}
			result.append( ch );
		}
		return result.toString();
	}
}

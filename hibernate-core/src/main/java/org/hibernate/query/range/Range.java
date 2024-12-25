/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.query.Restriction;

import java.util.List;

/**
 * Specifies an allowed set of range of values for a value being restricted.
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
	<X> Predicate toPredicate(Path<? extends X> root, SingularAttribute<X, U> attribute, CriteriaBuilder builder);

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
}

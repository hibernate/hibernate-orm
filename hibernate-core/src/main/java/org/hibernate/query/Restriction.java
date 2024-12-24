/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.query.range.Range;

/**
 * A rule for restricting query results.
 * <p>
 * This allows restrictions to be added to a {@link Query} by calling
 * {@link SelectionQuery#addRestriction(Restriction)}.
 * <pre>
 * session.createSelectionQuery("from Book", Book.class)
 *         .addRestriction(Restriction.like(Book_.title, "%Hibernate%", false))
 *         .addRestriction(Restriction.greaterThan(Book_.pages, 100))
 *         .setOrder(Order.desc(Book_.title))
 * 		   .getResultList() );
 * </pre>
 *
 * @param <X> The entity result type of the query
 *
 * @apiNote This class is similar to {@code jakarta.data.Restriction}, and
 *          is used by Hibernate Data Repositories to implement Jakarta Data
 *          query methods.
 *
 * @see SelectionQuery#addRestriction(Restriction)
 * @see Order
 *
 * @author Gavin King
 *
 * @since 7.0
 *
 */
@Incubating
public interface Restriction<X> {

	/**
	 * Negate this restriction.
	 */
	Restriction<X> negated();

	/**
	 * Return a JPA Criteria {@link Predicate} constraining the given
	 * root entity by this restriction.
	 */
	@Internal
	Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder);

	static <T,U> Restriction<T> restrict(SingularAttribute<T,U> attribute, Range<U> range) {
		return new AttributeRange<>( attribute, range );
	}

	static <T,U> Restriction<T> equal(SingularAttribute<T,U> attribute, U value) {
		return restrict( attribute, Range.singleValue(value) );
	}

	static <T,U> Restriction<T> unequal(SingularAttribute<T,U> attribute, U value) {
		return equal(attribute, value).negated();
	}

	static <T,U> Restriction<T> in(SingularAttribute<T,U> attribute, java.util.List<U> values) {
		return restrict( attribute, Range.valueList(values) );
	}

	static <T,U> Restriction<T> notIn(SingularAttribute<T,U> attribute, java.util.List<U> values) {
		return in(attribute, values).negated();
	}

	static <T,U extends Comparable<U>> Restriction<T> between(SingularAttribute<T,U> attribute, U lowerBound, U upperBound) {
		return restrict( attribute, Range.closed(lowerBound, upperBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> notBetween(SingularAttribute<T,U> attribute, U lowerBound, U upperBound) {
		return between(attribute, lowerBound, upperBound).negated();
	}

	static <T,U extends Comparable<U>> Restriction<T> greaterThan(SingularAttribute<T,U> attribute, U lowerBound) {
		return restrict( attribute, Range.greaterThan(lowerBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> lessThan(SingularAttribute<T,U> attribute, U upperBound) {
		return restrict( attribute, Range.lessThan(upperBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> greaterThanOrEqual(SingularAttribute<T,U> attribute, U lowerBound) {
		return restrict( attribute, Range.greaterThanOrEqualTo(lowerBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> lessThanOrEqual(SingularAttribute<T,U> attribute, U upperBound) {
		return restrict( attribute, Range.lessThanOrEqualTo(upperBound) );
	}

	static <T> Restriction<T> like(SingularAttribute<T,String> attribute, String pattern, boolean caseSensitive) {
		return restrict( attribute, Range.pattern(pattern, caseSensitive) );
	}

	static <T> Restriction<T> like(SingularAttribute<T,String> attribute, String pattern) {
		return like(attribute, pattern, true);
	}

	static <T> Restriction<T> notLike(SingularAttribute<T,String> attribute, String pattern) {
		return like(attribute, pattern, true).negated();
	}

	static <T> Restriction<T> notLike(SingularAttribute<T,String> attribute, String pattern, boolean caseSensitive) {
		return like(attribute, pattern, caseSensitive).negated();
	}

	@SafeVarargs
	static <T> Restriction<T> and(Restriction<T>... restrictions) {
		return new Conjunction<>( java.util.List.of(restrictions) );
	}

	@SafeVarargs
	static <T> Restriction<T> or(Restriction<T>... restrictions) {
		return new Disjunction<>( java.util.List.of(restrictions) );
	}
}

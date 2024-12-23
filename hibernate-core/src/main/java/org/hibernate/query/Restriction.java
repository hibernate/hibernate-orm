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
import org.hibernate.query.Domain.AttributeRestriction;
import org.hibernate.query.Domain.Conjunction;
import org.hibernate.query.Domain.Disjunction;
import org.hibernate.query.Domain.Interval;
import org.hibernate.query.Domain.List;
import org.hibernate.query.Domain.LowerBound;
import org.hibernate.query.Domain.Pattern;
import org.hibernate.query.Domain.UpperBound;
import org.hibernate.query.Domain.Value;

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

	static <T,U> Restriction<T> equal(SingularAttribute<T,U> attribute, U value) {
		return new AttributeRestriction<>( attribute, new Value<>(value) );
	}

	static <T,U> Restriction<T> unequal(SingularAttribute<T,U> attribute, U value) {
		return new AttributeRestriction<>( attribute, new Value<>(value) ).negated();
	}

	static <T,U> Restriction<T> in(SingularAttribute<T,U> attribute, java.util.List<U> values) {
		return new AttributeRestriction<>( attribute, new List<>(values) );
	}

	static <T,U> Restriction<T> notIn(SingularAttribute<T,U> attribute, java.util.List<U> values) {
		return new AttributeRestriction<>( attribute, new List<>(values) ).negated();
	}

	static <T,U extends Comparable<U>> Restriction<T> between(SingularAttribute<T,U> attribute, U lowerBound, U upperBound) {
		return new AttributeRestriction<>( attribute, Interval.closed(lowerBound, upperBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> notBetween(SingularAttribute<T,U> attribute, U lowerBound, U upperBound) {
		return new AttributeRestriction<>( attribute, Interval.closed(lowerBound, upperBound) ).negated();
	}

	static <T,U extends Comparable<U>> Restriction<T> greaterThan(SingularAttribute<T,U> attribute, U lowerBound) {
		return new AttributeRestriction<>( attribute, LowerBound.greaterThan(lowerBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> lessThan(SingularAttribute<T,U> attribute, U upperBound) {
		return new AttributeRestriction<>( attribute, UpperBound.lessThan(upperBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> greaterThanOrEqual(SingularAttribute<T,U> attribute, U lowerBound) {
		return new AttributeRestriction<>( attribute, LowerBound.greaterThanOrEqualTo(lowerBound) );
	}

	static <T,U extends Comparable<U>> Restriction<T> lessThanOrEqual(SingularAttribute<T,U> attribute, U upperBound) {
		return new AttributeRestriction<>( attribute, UpperBound.lessThanOrEqualTo(upperBound) );
	}

	static <T> Restriction<T> like(SingularAttribute<T,String> attribute, String pattern) {
		return like( attribute, pattern, true );
	}

	static <T> Restriction<T> like(SingularAttribute<T,String> attribute, String pattern, boolean caseSensitive) {
		return new AttributeRestriction<>( attribute, new Pattern(pattern, caseSensitive) );
	}

	static <T> Restriction<T> notLike(SingularAttribute<T,String> attribute, String pattern) {
		return notLike( attribute, pattern, true );
	}

	static <T> Restriction<T> notLike(SingularAttribute<T,String> attribute, String pattern, boolean caseSensitive) {
		return new AttributeRestriction<>( attribute, new Pattern(pattern, caseSensitive) ).negated();
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

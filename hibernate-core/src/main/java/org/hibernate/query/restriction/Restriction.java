/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.query.Order;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.range.Range;

import java.util.List;

/**
 * A rule for restricting query results. This allows restrictions to be added to
 * a {@link org.hibernate.query.specification.QuerySpecification} by calling
 * {@link org.hibernate.query.specification.QuerySpecification#restrict(Restriction)
 * restrict()}.
 * <pre>
 * SelectionSpecification.create(Book.class)
 *         .restrict(Restriction.like(Book_.title, "%Hibernate%", false))
 *         .restrict(Restriction.greaterThan(Book_.pages, 100))
 *         .sort(Order.desc(Book_.title))
 *         .createQuery(session)
 *         .getResultList();
 * </pre>
 * <p>
 * Each restriction pairs an {@linkplain SingularAttribute attribute} of the
 * entity with a {@link Range} of allowed values for the attribute.
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} or {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method} may be declared with type {@code Restriction<? super E>},
 * {@code List<Restriction<? super E>>}, or {@code Restriction<? super E>...}
 * (varargs) where {@code E} is the entity type returned by the query.
 * <p>
 * To create a {@code Restriction} on a compound path, use {@link Path}.
 *
 * @param <X> The entity result type of the query
 *
 * @apiNote This class is similar to {@code jakarta.data.Restriction}, and
 *          is used by Hibernate Data Repositories to implement Jakarta Data
 *          query methods.
 *
 * @see org.hibernate.query.specification.SelectionSpecification
 * @see org.hibernate.query.specification.MutationSpecification
 * @see org.hibernate.query.specification.QuerySpecification#restrict(Restriction)
 *
 * @see Path
 * @see Order
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating(since = "7.0", group = "query-specifications")
public interface Restriction<X> {

	/**
	 * Negate this restriction.
	 */
	@Nonnull
	Restriction<X> negated();

	/**
	 * Combine this restriction with the given restriction using logical or.
	 *
	 * @see #any(List)
	 */
	@Nonnull
	default Restriction<X> or(@Nonnull Restriction<X> restriction) {
		return any( this, restriction );
	}

	/**
	 * Combine this restriction with the given restriction using logical and.
	 *
	 * @see #all(List)
	 */
	@Nonnull
	default Restriction<X> and(@Nonnull Restriction<X> restriction) {
		return all( this, restriction );
	}

	/**
	 * Return a JPA Criteria {@link Predicate} constraining the given
	 * root entity by this restriction.
	 */
	@Internal
	@Nonnull
	Predicate toPredicate(@Nonnull Root<? extends X> root, @Nonnull CriteriaBuilder builder);

	/**
	 * Apply this restriction to the given root entity of the given
	 * {@linkplain CriteriaQuery criteria query}.
	 */
	default void apply(@Nonnull CriteriaQuery<?> query, @Nonnull Root<? extends X> root) {
		if ( !(query instanceof JpaCriteriaQuery<?> criteriaQuery) ) {
			throw new IllegalArgumentException( "Not a JpaCriteriaQuery" );
		}

		final var predicate = toPredicate( root, criteriaQuery.getCriteriaBuilder() );
		if ( query.getRestriction() == null ) {
			query.where( predicate );
		}
		else {
			query.where( query.getRestriction(), predicate );
		}
	}

	/**
	 * Restrict the allowed values of the given attribute to the given
	 * {@linkplain Range range}.
	 */
	@Nonnull
	static <T, U> Restriction<T> restrict(@Nonnull SingularAttribute<T, U> attribute, @Nonnull Range<U> range) {
		return new AttributeRange<>( attribute, range );
	}

	/**
	 * Restrict the allowed values of the named attribute of the given
	 * entity class to the given {@linkplain Range range}.
	 * <p>
	 * This operation is not compile-time type safe. Prefer the use of
	 * {@link #restrict(SingularAttribute, Range)}.
	 */
	@Nonnull
	static <T> Restriction<T> restrict(@Nonnull Class<T> type, @Nonnull String attributeName, @Nonnull Range<?> range) {
		return new NamedAttributeRange<>( type, attributeName, range );
	}

	/**
	 * Restrict the given attribute to be exactly equal to the given value.
	 *
	 * @see Range#singleValue(Object)
	 */
	@Nonnull
	static <T, U> Restriction<T> equal(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U value) {
		return restrict( attribute, Range.singleValue( value ) );
	}

	/**
	 * Restrict the given attribute to be not equal to the given value.
	 */
	@Nonnull
	static <T, U> Restriction<T> unequal(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U value) {
		return equal( attribute, value ).negated();
	}

	/**
	 * Restrict the given attribute to be equal to the given string, ignoring case.
	 *
	 * @see Range#singleCaseInsensitiveValue(String)
	 */
	@Nonnull
	static <T> Restriction<T> equalIgnoringCase(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String value) {
		return restrict( attribute, Range.singleCaseInsensitiveValue( value ) );
	}

	/**
	 * Restrict the given attribute to be exactly equal to one of the given values.
	 *
	 * @see Range#valueList(List)
	 */
	@SafeVarargs
	@Nonnull
	static <T, U> Restriction<T> in(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U... values) {
		return in( attribute, List.of(values ) );
	}

	/**
	 * Restrict the given attribute to be not equal to any of the given values.
	 */
	@SafeVarargs
	@Nonnull
	static <T, U> Restriction<T> notIn(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U... values) {
		return notIn( attribute, List.of(values ) );
	}

	/**
	 * Restrict the given attribute to be exactly equal to one of the given values.
	 *
	 * @see Range#valueList(List)
	 */
	@Nonnull
	static <T, U> Restriction<T> in(@Nonnull SingularAttribute<T, U> attribute, @Nonnull java.util.List<U> values) {
		return restrict( attribute, Range.valueList( values ) );
	}

	/**
	 * Restrict the given attribute to be not equal to any of the given values.
	 */
	@Nonnull
	static <T, U> Restriction<T> notIn(@Nonnull SingularAttribute<T, U> attribute, @Nonnull java.util.List<U> values) {
		return in( attribute, values ).negated();
	}

	/**
	 * Restrict the given attribute to fall between the given values.
	 *
	 * @see Range#closed(Comparable, Comparable)
	 */
	@Nonnull
	static <T, U extends Comparable<U>> Restriction<T> between(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U lowerBound, @Nonnull U upperBound) {
		return restrict( attribute, Range.closed( lowerBound, upperBound ) );
	}

	/**
	 * Restrict the given attribute to not fall between the given values.
	 */
	@Nonnull
	static <T, U extends Comparable<U>> Restriction<T> notBetween(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U lowerBound, @Nonnull U upperBound) {
		return between( attribute, lowerBound, upperBound ).negated();
	}

	/**
	 * Restrict the given attribute to be strictly greater than the given lower bound.
	 *
	 * @see Range#greaterThan(Comparable)
	 */
	@Nonnull
	static <T, U extends Comparable<U>> Restriction<T> greaterThan(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U lowerBound) {
		return restrict( attribute, Range.greaterThan( lowerBound ) );
	}

	/**
	 * Restrict the given attribute to be strictly less than the given upper bound.
	 *
	 * @see Range#lessThan(Comparable)
	 */
	@Nonnull
	static <T, U extends Comparable<U>> Restriction<T> lessThan(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U upperBound) {
		return restrict( attribute, Range.lessThan( upperBound ) );
	}

	/**
	 * Restrict the given attribute to be greater than or equal to the given lower bound.
	 *
	 * @see Range#greaterThanOrEqualTo(Comparable)
	 */
	@Nonnull
	static <T, U extends Comparable<U>> Restriction<T> greaterThanOrEqual(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U lowerBound) {
		return restrict( attribute, Range.greaterThanOrEqualTo( lowerBound ) );
	}

	/**
	 * Restrict the given attribute to be less than or equal to the given upper bound.
	 *
	 * @see Range#lessThanOrEqualTo(Comparable)
	 */
	@Nonnull
	static <T, U extends Comparable<U>> Restriction<T> lessThanOrEqual(@Nonnull SingularAttribute<T, U> attribute, @Nonnull U upperBound) {
		return restrict( attribute, Range.lessThanOrEqualTo( upperBound ) );
	}

	/**
	 * Restrict the given attribute to match the given pattern, explicitly specifying
	 * case sensitivity, along with single-character and multi-character wildcards.
	 *
	 * @param pattern A pattern involving the given wildcard characters
	 * @param caseSensitive {@code true} if matching is case-sensitive
	 * @param charWildcard A wildcard character which matches any single character
	 * @param stringWildcard A wildcard character which matches any string of characters
	 *
	 * @see Range#pattern(String, boolean, char, char)
	 */
	@Nonnull
	static <T> Restriction<T> like(
			@Nonnull SingularAttribute<T, String> attribute,
			@Nonnull String pattern, boolean caseSensitive,
			char charWildcard, char stringWildcard) {
		return restrict( attribute, Range.pattern( pattern, caseSensitive, charWildcard, stringWildcard ) );
	}

	/**
	 * Restrict the given attribute to match the given pattern, explicitly specifying
	 * case sensitivity. The pattern must be expressed in terms of the default wildcard
	 * characters {@code _} and {@code %}.
	 *
	 * @param pattern A pattern involving the default wildcard characters
	 * @param caseSensitive {@code true} if matching is case-sensitive
	 *
	 * @see Range#pattern(String, boolean)
	 */
	@Nonnull
	static <T> Restriction<T> like(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String pattern, boolean caseSensitive) {
		return restrict( attribute, Range.pattern( pattern, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to match the given pattern. The pattern must be
	 * expressed in terms of the default wildcard characters {@code _} and {@code %}.
	 *
	 * @see Range#pattern(String)
	 */
	@Nonnull
	static <T> Restriction<T> like(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String pattern) {
		return like( attribute, pattern, true );
	}

	/**
	 * Restrict the given attribute to not match the given pattern. The pattern must
	 * be expressed in terms of the default wildcard characters {@code _} and {@code %}.
	 *
	 * @see Range#pattern(String)
	 */
	@Nonnull
	static <T> Restriction<T> notLike(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String pattern) {
		return like( attribute, pattern, true ).negated();
	}

	/**
	 * Restrict the given attribute to not match the given pattern, explicitly specifying
	 * case sensitivity. The pattern must be expressed in terms of the default wildcard
	 * characters {@code _} and {@code %}.
	 *
	 * @param pattern A pattern involving the default wildcard characters
	 * @param caseSensitive {@code true} if matching is case-sensitive
	 */
	@Nonnull
	static <T> Restriction<T> notLike(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String pattern, boolean caseSensitive) {
		return like( attribute, pattern, caseSensitive ).negated();
	}

	/**
	 * Restrict the given attribute to start with the given string prefix.
	 *
	 * @see Range#prefix(String)
	 */
	@Nonnull
	static <T> Restriction<T> startsWith(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String prefix) {
		return startsWith( attribute, prefix, true );
	}

	/**
	 * Restrict the given attribute to end with the given string suffix.
	 *
	 * @see Range#suffix(String)
	 */
	@Nonnull
	static <T> Restriction<T> endsWith(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String suffix) {
		return endsWith( attribute, suffix, true );
	}

	/**
	 * Restrict the given attribute to contain the given substring.
	 *
	 * @see Range#containing(String)
	 */
	@Nonnull
	static <T> Restriction<T> contains(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String substring) {
		return contains( attribute, substring, true );
	}

	/**
	 * Restrict the given attribute to not contain the given substring.
	 */
	@Nonnull
	static <T> Restriction<T> notContains(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String substring) {
		return notContains( attribute, substring, true );
	}

	/**
	 * Restrict the given attribute to start with the given string prefix, explicitly
	 * specifying case sensitivity.
	 *
	 * @see Range#prefix(String, boolean)
	 */
	@Nonnull
	static <T> Restriction<T> startsWith(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String prefix, boolean caseSensitive) {
		return restrict( attribute, Range.prefix( prefix, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to end with the given string suffix, explicitly
	 * specifying case sensitivity.
	 *
	 * @see Range#suffix(String, boolean)
	 */
	@Nonnull
	static <T> Restriction<T> endsWith(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String suffix, boolean caseSensitive) {
		return restrict( attribute, Range.suffix( suffix, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to contain the given substring, explicitly
	 * specifying case sensitivity.
	 *
	 * @see Range#containing(String, boolean)
	 */
	@Nonnull
	static <T> Restriction<T> contains(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String substring, boolean caseSensitive) {
		return restrict( attribute, Range.containing( substring, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to not contain the given substring, explicitly
	 * specifying case sensitivity.
	 */
	@Nonnull
	static <T> Restriction<T> notContains(@Nonnull SingularAttribute<T, String> attribute, @Nonnull String substring, boolean caseSensitive) {
		return contains( attribute, substring, caseSensitive ).negated();
	}

	/**
	 * Restrict the given attribute to be non-null.
	 */
	@Nonnull
	static <T, U> Restriction<T> notNull(@Nonnull SingularAttribute<T, U> attribute) {
		return restrict( attribute, Range.notNull( attribute.getJavaType() ) );
	}

	/**
	 * Combine the given restrictions using logical and.
	 */
	@Nonnull
	static <T> Restriction<T> all(@Nonnull List<? extends Restriction<? super T>> restrictions) {
		return new Conjunction<>( restrictions );
	}

	/**
	 * Combine the given restrictions using logical or.
	 */
	@Nonnull
	static <T> Restriction<T> any(@Nonnull List<? extends Restriction<? super T>> restrictions) {
		return new Disjunction<>( restrictions );
	}

	/**
	 * Combine the given restrictions using logical and.
	 */
	@SafeVarargs
	@Nonnull
	static <T> Restriction<T> all(@Nonnull Restriction<? super T>... restrictions) {
		return new Conjunction<T>( List.of( restrictions ) );
	}

	/**
	 * Combine the given restrictions using logical or.
	 */
	@SafeVarargs
	@Nonnull
	static <T> Restriction<T> any(@Nonnull Restriction<? super T>... restrictions) {
		return new Disjunction<T>( List.of( restrictions ) );
	}

	/**
	 * An empty restriction.
	 */
	@Nonnull
	static <T> Restriction<T> unrestricted() {
		return new Unrestricted<>();
	}
}

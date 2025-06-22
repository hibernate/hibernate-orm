/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

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
@Incubating
public interface Restriction<X> {

	/**
	 * Negate this restriction.
	 */
	Restriction<X> negated();

	/**
	 * Combine this restriction with the given restriction using logical or.
	 *
	 * @see #any(List)
	 */
	default Restriction<X> or(Restriction<X> restriction) {
		return any( this, restriction );
	}

	/**
	 * Combine this restriction with the given restriction using logical and.
	 *
	 * @see #all(List)
	 */
	default Restriction<X> and(Restriction<X> restriction) {
		return all( this, restriction );
	}

	/**
	 * Return a JPA Criteria {@link Predicate} constraining the given
	 * root entity by this restriction.
	 */
	@Internal
	Predicate toPredicate(Root<? extends X> root, CriteriaBuilder builder);

	/**
	 * Apply this restriction to the given root entity of the given
	 * {@linkplain CriteriaQuery criteria query}.
	 */
	default void apply(CriteriaQuery<?> query, Root<? extends X> root) {
		if ( !(query instanceof JpaCriteriaQuery<?> criteriaQuery) ) {
			throw new IllegalArgumentException( "Not a JpaCriteriaQuery" );
		}
		query.where( query.getRestriction(), toPredicate( root, criteriaQuery.getCriteriaBuilder() ) );
	}

	/**
	 * Restrict the allowed values of the given attribute to the given
	 * {@linkplain Range range}.
	 */
	static <T, U> Restriction<T> restrict(SingularAttribute<T, U> attribute, Range<U> range) {
		return new AttributeRange<>( attribute, range );
	}

	/**
	 * Restrict the allowed values of the named attribute of the given
	 * entity class to the given {@linkplain Range range}.
	 * <p>
	 * This operation is not compile-time type safe. Prefer the use of
	 * {@link #restrict(SingularAttribute, Range)}.
	 */
	static <T> Restriction<T> restrict(Class<T> type, String attributeName, Range<?> range) {
		return new NamedAttributeRange<>( type, attributeName, range );
	}

	/**
	 * Restrict the given attribute to be exactly equal to the given value.
	 *
	 * @see Range#singleValue(Object)
	 */
	static <T, U> Restriction<T> equal(SingularAttribute<T, U> attribute, U value) {
		return restrict( attribute, Range.singleValue( value ) );
	}

	/**
	 * Restrict the given attribute to be not equal to the given value.
	 */
	static <T, U> Restriction<T> unequal(SingularAttribute<T, U> attribute, U value) {
		return equal( attribute, value ).negated();
	}

	/**
	 * Restrict the given attribute to be equal to the given string, ignoring case.
	 *
	 * @see Range#singleCaseInsensitiveValue(String)
	 */
	static <T> Restriction<T> equalIgnoringCase(SingularAttribute<T, String> attribute, String value) {
		return restrict( attribute, Range.singleCaseInsensitiveValue( value ) );
	}

	/**
	 * Restrict the given attribute to be exactly equal to one of the given values.
	 *
	 * @see Range#valueList(List)
	 */
	@SafeVarargs
	static <T, U> Restriction<T> in(SingularAttribute<T, U> attribute, U... values) {
		return in( attribute, List.of(values ) );
	}

	/**
	 * Restrict the given attribute to be not equal to any of the given values.
	 */
	@SafeVarargs
	static <T, U> Restriction<T> notIn(SingularAttribute<T, U> attribute, U... values) {
		return notIn( attribute, List.of(values ) );
	}

	/**
	 * Restrict the given attribute to be exactly equal to one of the given values.
	 *
	 * @see Range#valueList(List)
	 */
	static <T, U> Restriction<T> in(SingularAttribute<T, U> attribute, java.util.List<U> values) {
		return restrict( attribute, Range.valueList( values ) );
	}

	/**
	 * Restrict the given attribute to be not equal to any of the given values.
	 */
	static <T, U> Restriction<T> notIn(SingularAttribute<T, U> attribute, java.util.List<U> values) {
		return in( attribute, values ).negated();
	}

	/**
	 * Restrict the given attribute to fall between the given values.
	 *
	 * @see Range#closed(Comparable, Comparable)
	 */
	static <T, U extends Comparable<U>> Restriction<T> between(SingularAttribute<T, U> attribute, U lowerBound, U upperBound) {
		return restrict( attribute, Range.closed( lowerBound, upperBound ) );
	}

	/**
	 * Restrict the given attribute to not fall between the given values.
	 */
	static <T, U extends Comparable<U>> Restriction<T> notBetween(SingularAttribute<T, U> attribute, U lowerBound, U upperBound) {
		return between( attribute, lowerBound, upperBound ).negated();
	}

	/**
	 * Restrict the given attribute to be strictly greater than the given lower bound.
	 *
	 * @see Range#greaterThan(Comparable)
	 */
	static <T, U extends Comparable<U>> Restriction<T> greaterThan(SingularAttribute<T, U> attribute, U lowerBound) {
		return restrict( attribute, Range.greaterThan( lowerBound ) );
	}

	/**
	 * Restrict the given attribute to be strictly less than the given upper bound.
	 *
	 * @see Range#lessThan(Comparable)
	 */
	static <T, U extends Comparable<U>> Restriction<T> lessThan(SingularAttribute<T, U> attribute, U upperBound) {
		return restrict( attribute, Range.lessThan( upperBound ) );
	}

	/**
	 * Restrict the given attribute to be greater than or equal to the given lower bound.
	 *
	 * @see Range#greaterThanOrEqualTo(Comparable)
	 */
	static <T, U extends Comparable<U>> Restriction<T> greaterThanOrEqual(SingularAttribute<T, U> attribute, U lowerBound) {
		return restrict( attribute, Range.greaterThanOrEqualTo( lowerBound ) );
	}

	/**
	 * Restrict the given attribute to be less than or equal to the given upper bound.
	 *
	 * @see Range#lessThanOrEqualTo(Comparable)
	 */
	static <T, U extends Comparable<U>> Restriction<T> lessThanOrEqual(SingularAttribute<T, U> attribute, U upperBound) {
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
	static <T> Restriction<T> like(
			SingularAttribute<T, String> attribute,
			String pattern, boolean caseSensitive,
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
	static <T> Restriction<T> like(SingularAttribute<T, String> attribute, String pattern, boolean caseSensitive) {
		return restrict( attribute, Range.pattern( pattern, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to match the given pattern. The pattern must be
	 * expressed in terms of the default wildcard characters {@code _} and {@code %}.
	 *
	 * @see Range#pattern(String)
	 */
	static <T> Restriction<T> like(SingularAttribute<T, String> attribute, String pattern) {
		return like( attribute, pattern, true );
	}

	/**
	 * Restrict the given attribute to not match the given pattern. The pattern must
	 * be expressed in terms of the default wildcard characters {@code _} and {@code %}.
	 *
	 * @see Range#pattern(String)
	 */
	static <T> Restriction<T> notLike(SingularAttribute<T, String> attribute, String pattern) {
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
	static <T> Restriction<T> notLike(SingularAttribute<T, String> attribute, String pattern, boolean caseSensitive) {
		return like( attribute, pattern, caseSensitive ).negated();
	}

	/**
	 * Restrict the given attribute to start with the given string prefix.
	 *
	 * @see Range#prefix(String)
	 */
	static <T> Restriction<T> startsWith(SingularAttribute<T, String> attribute, String prefix) {
		return startsWith( attribute, prefix, true );
	}

	/**
	 * Restrict the given attribute to end with the given string suffix.
	 *
	 * @see Range#suffix(String)
	 */
	static <T> Restriction<T> endsWith(SingularAttribute<T, String> attribute, String suffix) {
		return endsWith( attribute, suffix, true );
	}

	/**
	 * Restrict the given attribute to contain the given substring.
	 *
	 * @see Range#containing(String)
	 */
	static <T> Restriction<T> contains(SingularAttribute<T, String> attribute, String substring) {
		return contains( attribute, substring, true );
	}

	/**
	 * Restrict the given attribute to not contain the given substring.
	 */
	static <T> Restriction<T> notContains(SingularAttribute<T, String> attribute, String substring) {
		return notContains( attribute, substring, true );
	}

	/**
	 * Restrict the given attribute to start with the given string prefix, explicitly
	 * specifying case sensitivity.
	 *
	 * @see Range#prefix(String, boolean)
	 */
	static <T> Restriction<T> startsWith(SingularAttribute<T, String> attribute, String prefix, boolean caseSensitive) {
		return restrict( attribute, Range.prefix( prefix, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to end with the given string suffix, explicitly
	 * specifying case sensitivity.
	 *
	 * @see Range#suffix(String, boolean)
	 */
	static <T> Restriction<T> endsWith(SingularAttribute<T, String> attribute, String suffix, boolean caseSensitive) {
		return restrict( attribute, Range.suffix( suffix, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to contain the given substring, explicitly
	 * specifying case sensitivity.
	 *
	 * @see Range#containing(String, boolean)
	 */
	static <T> Restriction<T> contains(SingularAttribute<T, String> attribute, String substring, boolean caseSensitive) {
		return restrict( attribute, Range.containing( substring, caseSensitive ) );
	}

	/**
	 * Restrict the given attribute to not contain the given substring, explicitly
	 * specifying case sensitivity.
	 */
	static <T> Restriction<T> notContains(SingularAttribute<T, String> attribute, String substring, boolean caseSensitive) {
		return contains( attribute, substring, caseSensitive ).negated();
	}

	/**
	 * Restrict the given attribute to be non-null.
	 */
	static <T, U> Restriction<T> notNull(SingularAttribute<T, U> attribute) {
		return restrict( attribute, Range.notNull( attribute.getJavaType() ) );
	}

	/**
	 * Combine the given restrictions using logical and.
	 */
	static <T> Restriction<T> all(List<? extends Restriction<? super T>> restrictions) {
		return new Conjunction<>( restrictions );
	}

	/**
	 * Combine the given restrictions using logical or.
	 */
	static <T> Restriction<T> any(List<? extends Restriction<? super T>> restrictions) {
		return new Disjunction<>( restrictions );
	}

	/**
	 * Combine the given restrictions using logical and.
	 */
	@SafeVarargs
	static <T> Restriction<T> all(Restriction<? super T>... restrictions) {
		return new Conjunction<T>( java.util.List.of( restrictions ) );
	}

	/**
	 * Combine the given restrictions using logical or.
	 */
	@SafeVarargs
	static <T> Restriction<T> any(Restriction<? super T>... restrictions) {
		return new Disjunction<T>( java.util.List.of( restrictions ) );
	}

	/**
	 * An empty restriction.
	 */
	static <T> Restriction<T> unrestricted() {
		return new Unrestricted<>();
	}
}

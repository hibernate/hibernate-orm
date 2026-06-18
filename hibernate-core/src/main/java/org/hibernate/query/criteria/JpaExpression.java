/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Subquery;

/**
 * API extension to the JPA {@link Expression} contract
 *
 * @author Steve Ebersole
 */
public interface JpaExpression<T> extends JpaSelection<T>, Expression<T> {

	/**
	 * Convert this expression to Long.
	 */
	@Nonnull
	JpaExpression<Long> asLong();

	/**
	 * Convert this expression to Integer.
	 */
	@Nonnull
	JpaExpression<Integer> asInteger();

	/**
	 * Convert this expression to Float.
	 */
	@Nonnull
	JpaExpression<Float> asFloat();

	/**
	 * Convert this expression to Double.
	 */
	@Nonnull
	JpaExpression<Double> asDouble();

	/**
	 * Convert this expression to BigDecimal.
	 */
	@Nonnull
	JpaExpression<BigDecimal> asBigDecimal();

	/**
	 * Convert this expression to BigInteger.
	 */
	@Nonnull
	JpaExpression<BigInteger> asBigInteger();

	/**
	 * Convert this expression to String.
	 */
	@Nonnull
	JpaExpression<String> asString();

	/**
	 * Narrow this expression to the given Java type.
	 */
	@Nonnull
	@Override
	<X> JpaExpression<X> as(@Nonnull Class<X> type);

	/**
	 * Create a predicate testing whether this expression is null.
	 */
	@Nonnull
	@Override
	JpaPredicate isNull();

	/**
	 * Create a predicate testing whether this expression is not null.
	 */
	@Nonnull
	@Override
	JpaPredicate isNotNull();

	/**
	 * Create an in predicate for this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Object... values);

	/**
	 * Create an in predicate for this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Expression<?>... values);

	/**
	 * Create an in predicate for this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Collection<?> values);

	/**
	 * Create an in predicate for this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Expression<Collection<?>> values);

	/**
	 * Create an in predicate for this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Subquery<T> subquery);

	/**
	 * Create a coalesce expression using this expression.
	 */
	@Nonnull
	@Override
	JpaExpression<T> coalesce(@Nonnull Expression<? extends T> y);

	/**
	 * Create a coalesce expression using this expression.
	 */
	@Nonnull
	@Override
	JpaExpression<T> coalesce(T y);

	/**
	 * Create a simple case expression based on this expression.
	 */
	<R> JpaSimpleCase<T, R> selectCase();

	/**
	 * Create a simple case expression based on this expression.
	 */
	@Nonnull
	@Override
	<R> JpaSimpleCase<T, R> selectCase(@Nonnull Class<R> resultType);

	/**
	 * Create a count expression for this expression.
	 */
	@Nonnull
	@Override
	JpaNumericExpression<Long> count();

	/**
	 * Create a distinct count expression for this expression.
	 */
	@Nonnull
	@Override
	JpaNumericExpression<Long> countDistinct();

	/**
	 * Create a nullif expression using this expression.
	 */
	@Nonnull
	@Override
	JpaExpression<T> nullif(@Nonnull Expression<? extends T> y);

	/**
	 * Create a nullif expression using this expression.
	 */
	@Nonnull
	@Override
	JpaExpression<T> nullif(T y);

	/**
	 * Create a predicate testing whether this expression is a member of a collection.
	 */
	@Nonnull
	@Override
	JpaPredicate isMember(@Nonnull Expression<? extends Collection<? super T>> collection);

	/**
	 * Create a predicate testing whether this expression is not a member of a collection.
	 */
	@Nonnull
	@Override
	JpaPredicate isNotMember(@Nonnull Expression<? extends Collection<? super T>> collection);

	/**
	 * Create a predicate testing equality with this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate equalTo(@Nonnull Expression<?> value);

	/**
	 * Create a predicate testing equality with this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate equalTo(Object value);

	/**
	 * Create a cast expression for this expression.
	 */
	@Nonnull
	@Override
	<X> JpaExpression<X> cast(@Nonnull Class<X> type);

	/**
	 * Create a predicate testing inequality with this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate notEqualTo(@Nonnull Expression<?> value);

	/**
	 * Create a predicate testing inequality with this expression.
	 */
	@Nonnull
	@Override
	JpaPredicate notEqualTo(Object value);
}

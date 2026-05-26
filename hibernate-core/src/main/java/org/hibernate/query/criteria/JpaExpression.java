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

	@Nonnull
	JpaExpression<Long> asLong();

	@Nonnull
	JpaExpression<Integer> asInteger();

	@Nonnull
	JpaExpression<Float> asFloat();

	@Nonnull
	JpaExpression<Double> asDouble();

	@Nonnull
	JpaExpression<BigDecimal> asBigDecimal();

	@Nonnull
	JpaExpression<BigInteger> asBigInteger();

	@Nonnull
	JpaExpression<String> asString();

	@Nonnull
	@Override
	<X> JpaExpression<X> as(@Nonnull Class<X> type);

	@Nonnull
	@Override
	JpaPredicate isNull();

	@Nonnull
	@Override
	JpaPredicate isNotNull();

	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Object... values);

	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Expression<?>... values);

	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Collection<?> values);

	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Expression<Collection<?>> values);

	@Nonnull
	@Override
	JpaPredicate in(@Nonnull Subquery<T> subquery);

	@Nonnull
	@Override
	JpaExpression<T> coalesce(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	JpaExpression<T> coalesce(T y);

	<R> JpaSimpleCase<T, R> selectCase();

	@Nonnull
	@Override
	<R> JpaSimpleCase<T, R> selectCase(@Nonnull Class<R> resultType);

	@Nonnull
	@Override
	JpaNumericExpression<Long> count();

	@Nonnull
	@Override
	JpaNumericExpression<Long> countDistinct();

	@Nonnull
	@Override
	JpaExpression<T> nullif(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	JpaExpression<T> nullif(T y);

	@Nonnull
	@Override
	JpaPredicate isMember(@Nonnull Expression<? extends Collection<? super T>> collection);

	@Nonnull
	@Override
	JpaPredicate isNotMember(@Nonnull Expression<? extends Collection<? super T>> collection);

	@Nonnull
	@Override
	JpaPredicate equalTo(@Nonnull Expression<?> value);

	@Nonnull
	@Override
	JpaPredicate equalTo(Object value);

	@Nonnull
	@Override
	<X> JpaExpression<X> cast(@Nonnull Class<X> type);

	@Nonnull
	@Override
	JpaPredicate notEqualTo(@Nonnull Expression<?> value);

	@Nonnull
	@Override
	JpaPredicate notEqualTo(Object value);
}

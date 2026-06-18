/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaNumericExpression;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Steve Ebersole
 */
public interface SqmNumericExpression<N extends Number & Comparable<N>>
		extends SqmComparableExpression<N>, JpaNumericExpression<N> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NumericExpression

	@Nonnull
	@Override
	SqmNumericExpression<N> coalesce(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> coalesce(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> nullif(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> nullif(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate gt(Number y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate ge(Number y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate lt(Number y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate le(Number y);

	@Nonnull
	@Override
	SqmNumericExpression<Integer> sign();

	@Nonnull
	@Override
	SqmNumericExpression<N> negated();

	@Nonnull
	@Override
	SqmNumericExpression<N> abs();

	@Nonnull
	@Override
	SqmNumericExpression<N> ceiling();

	@Nonnull
	@Override
	SqmNumericExpression<N> floor();

	@Nonnull
	@Override
	SqmNumericExpression<N> plus(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> plus(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> times(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> times(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> minus(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> minus(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> dividedBy(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> dividedBy(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> subtractedFrom(N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> dividedInto(N y);

	@Nonnull
	@Override
	SqmNumericExpression<Double> sqrt();

	@Nonnull
	@Override
	SqmNumericExpression<Double> exp();

	@Nonnull
	@Override
	SqmNumericExpression<Double> ln();

	@Nonnull
	@Override
	SqmNumericExpression<Double> power(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmNumericExpression<Double> power(Number y);

	@Nonnull
	@Override
	SqmNumericExpression<N> round(@Nonnull Integer n);

	@Nonnull
	@Override
	SqmNumericExpression<Double> avg();

	@Nonnull
	@Override
	SqmNumericExpression<N> sum();

	@Nonnull
	@Override
	SqmNumericExpression<Long> sumAsLong();

	@Nonnull
	@Override
	SqmNumericExpression<Double> sumAsDouble();

	@Nonnull
	@Override
	SqmNumericExpression<N> max();

	@Nonnull
	@Override
	SqmNumericExpression<N> min();

	@Nonnull
	@Override
	SqmNumericExpression<Long> toLong();

	@Nonnull
	@Override
	SqmNumericExpression<Integer> toInteger();

	@Nonnull
	@Override
	SqmNumericExpression<Float> toFloat();

	@Nonnull
	@Override
	SqmNumericExpression<Double> toDouble();

	@Nonnull
	@Override
	SqmNumericExpression<BigDecimal> toBigDecimal();

	@Nonnull
	@Override
	SqmNumericExpression<BigInteger> toBigInteger();
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaNumericExpression;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Steve Ebersole
 */
public interface SqmNumericExpression<N extends Number & Comparable<N>>
		extends SqmComparableExpression<N>, JpaNumericExpression<N> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NumericExpression

	@Override
	SqmNumericExpression<N> coalesce(N y);

	@Override
	SqmNumericExpression<N> coalesce(Expression<? extends N> y);

	@Override
	SqmNumericExpression<N> nullif(N y);

	@Override
	SqmNumericExpression<N> nullif(Expression<? extends N> y);

	@Override
	SqmPredicate gt(Expression<? extends Number> y);

	@Override
	SqmPredicate gt(Number y);

	@Override
	SqmPredicate ge(Expression<? extends Number> y);

	@Override
	SqmPredicate ge(Number y);

	@Override
	SqmPredicate lt(Expression<? extends Number> y);

	@Override
	SqmPredicate lt(Number y);

	@Override
	SqmPredicate le(Expression<? extends Number> y);

	@Override
	SqmPredicate le(Number y);

	@Override
	SqmNumericExpression<Integer> sign();

	@Override
	SqmNumericExpression<N> negated();

	@Override
	SqmNumericExpression<N> abs();

	@Override
	SqmNumericExpression<N> ceiling();

	@Override
	SqmNumericExpression<N> floor();

	@Override
	SqmNumericExpression<N> plus(Expression<? extends N> y);

	@Override
	SqmNumericExpression<N> plus(N y);

	@Override
	SqmNumericExpression<N> times(Expression<? extends N> y);

	@Override
	SqmNumericExpression<N> times(N y);

	@Override
	SqmNumericExpression<N> minus(Expression<? extends N> y);

	@Override
	SqmNumericExpression<N> minus(N y);

	@Override
	SqmNumericExpression<N> dividedBy(Expression<? extends N> y);

	@Override
	SqmNumericExpression<N> dividedBy(N y);

	@Override
	SqmNumericExpression<N> subtractedFrom(N y);

	@Override
	SqmNumericExpression<N> dividedInto(N y);

	@Override
	SqmNumericExpression<Double> sqrt();

	@Override
	SqmNumericExpression<Double> exp();

	@Override
	SqmNumericExpression<Double> ln();

	@Override
	SqmNumericExpression<Double> power(Expression<? extends Number> y);

	@Override
	SqmNumericExpression<Double> power(Number y);

	@Override
	SqmNumericExpression<N> round(Integer n);

	@Override
	SqmNumericExpression<Double> avg();

	@Override
	SqmNumericExpression<N> sum();

	@Override
	SqmNumericExpression<Long> sumAsLong();

	@Override
	SqmNumericExpression<Double> sumAsDouble();

	@Override
	SqmNumericExpression<N> max();

	@Override
	SqmNumericExpression<N> min();

	@Override
	SqmNumericExpression<Long> toLong();

	@Override
	SqmNumericExpression<Integer> toInteger();

	@Override
	SqmNumericExpression<Float> toFloat();

	@Override
	SqmNumericExpression<Double> toDouble();

	@Override
	SqmNumericExpression<BigDecimal> toBigDecimal();

	@Override
	SqmNumericExpression<BigInteger> toBigInteger();
}

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
	SqmNumericExpression<N> coalesce(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> coalesce(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> nullif(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> nullif(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Number y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Number y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Number y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Number y);

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
	SqmNumericExpression<N> plus(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> times(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> times(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> minus(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> minus(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> dividedBy(@Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmNumericExpression<N> dividedBy(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> subtractedFrom(@Nonnull N y);

	@Nonnull
	@Override
	SqmNumericExpression<N> dividedInto(@Nonnull N y);

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
	SqmNumericExpression<Double> power(@Nonnull Number y);

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

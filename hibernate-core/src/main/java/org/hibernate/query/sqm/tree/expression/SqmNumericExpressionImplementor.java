/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Steve Ebersole
 */
public interface SqmNumericExpressionImplementor<N extends Number & Comparable<N>>
		extends SqmComparableExpressionImplementor<N>, SqmNumericExpression<N> {
	SqmCriteriaNodeBuilder nodeBuilder();

	@Nonnull
	@Override
	default SqmNumericExpression<N> coalesce(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().coalesce(this, y ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> coalesce(N y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().coalesce(this, y ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> nullif(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().nullif(this, y ) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> nullif(N y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().nullif(this, y ) );
	}

	@Nonnull
	@Override
	default SqmPredicate gt(@Nonnull Expression<? extends Number> y) {
		return nodeBuilder().gt( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate gt(Number y) {
		return nodeBuilder().gt( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate ge(@Nonnull Expression<? extends Number> y) {
		return nodeBuilder().ge( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate ge(Number y) {
		return nodeBuilder().ge( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate lt(@Nonnull Expression<? extends Number> y) {
		return nodeBuilder().lt( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate lt(Number y) {
		return nodeBuilder().lt( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate le(@Nonnull Expression<? extends Number> y) {
		return nodeBuilder().le( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate le(Number y) {
		return nodeBuilder().le( this, y );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Integer> sign() {
		var expr = nodeBuilder().sign( this );
		return expr instanceof SqmNumericExpression<Integer> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> negated() {
		var expr = nodeBuilder().neg( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> abs() {
		var expr = nodeBuilder().abs( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> ceiling() {
		var expr = nodeBuilder().ceiling( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> floor() {
		var expr = nodeBuilder().floor( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> plus(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.ADD,
				this,
				(SqmExpression<?>) y,
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> plus(N y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.ADD,
				this,
				nodeBuilder().numericLiteral( y ),
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> times(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.MULTIPLY,
				this,
				(SqmExpression<?>) y,
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> times(N y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.MULTIPLY,
				this,
				nodeBuilder().numericLiteral( y ),
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> minus(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.SUBTRACT,
				this,
				(SqmExpression<?>) y,
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> minus(N y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.SUBTRACT,
				this,
				nodeBuilder().numericLiteral( y ),
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> dividedBy(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.DIVIDE,
				this,
				(SqmExpression<?>) y,
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> dividedBy(N y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.DIVIDE,
				this,
				nodeBuilder().numericLiteral( y ),
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> subtractedFrom(N y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.SUBTRACT,
				nodeBuilder().numericLiteral( y ),
				this,
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> dividedInto(N y) {
		return new SqmNumericExpressionWrapper<>( new SqmBinaryArithmetic<N>(
				BinaryArithmeticOperator.DIVIDE,
				nodeBuilder().numericLiteral( y ),
				this,
				nodeBuilder()
		) );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> sqrt() {
		var expr = nodeBuilder().sqrt( this );
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> exp() {
		var expr = nodeBuilder().exp( this );
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> ln() {
		var expr = nodeBuilder().ln( this );
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> power(@Nonnull Expression<? extends Number> y) {
		var expr = nodeBuilder().power( this, y );
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> power(Number y) {
		var expr = nodeBuilder().power( this, y );
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> round(@Nonnull Integer n) {
		var expr = nodeBuilder().round( this, n );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> max() {
		var expr = nodeBuilder().max( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> min() {
		var expr = nodeBuilder().min( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> avg() {
		var expr = nodeBuilder().avg( this );
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<N> sum() {
		var expr = nodeBuilder().sum( this );
		return expr instanceof SqmNumericExpression<N> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Long> sumAsLong() {
		var expr = nodeBuilder().sum( this ).asLong();
		return expr instanceof SqmNumericExpression<Long> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> sumAsDouble() {
		var expr = nodeBuilder().sum( this ).asDouble();
		return expr instanceof SqmNumericExpression<Double> correct
				? correct
				: new SqmNumericExpressionWrapper<>( expr );
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Long> toLong() {
		if ( Long.class.equals( getJavaType() ) ) {
			//noinspection unchecked,rawtypes
			return (SqmNumericExpression) this;
		}
		else {
			return new SqmNumericExpressionWrapper<>( asLong() );
		}
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Integer> toInteger() {
		if ( Integer.class.equals( getJavaType() ) ) {
			//noinspection unchecked,rawtypes
			return (SqmNumericExpression) this;
		}
		else {
			return new SqmNumericExpressionWrapper<>( asInteger() );
		}
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Float> toFloat() {
		if ( Float.class.equals( getJavaType() ) ) {
			//noinspection unchecked,rawtypes
			return (SqmNumericExpression) this;
		}
		else {
			return new SqmNumericExpressionWrapper<>( asFloat() );
		}
	}

	@Nonnull
	@Override
	default SqmNumericExpression<Double> toDouble() {
		if ( Double.class.equals( getJavaType() ) ) {
			//noinspection unchecked,rawtypes
			return (SqmNumericExpression) this;
		}
		else {
			return new SqmNumericExpressionWrapper<>( asDouble() );
		}
	}

	@Nonnull
	@Override
	default SqmNumericExpression<BigDecimal> toBigDecimal() {
		if ( BigDecimal.class.equals( getJavaType() ) ) {
			//noinspection unchecked,rawtypes
			return (SqmNumericExpression) this;
		}
		else {
			return new SqmNumericExpressionWrapper<>( asBigDecimal() );
		}
	}

	@Nonnull
	@Override
	default SqmNumericExpression<BigInteger> toBigInteger() {
		if ( BigInteger.class.equals( getJavaType() ) ) {
			//noinspection unchecked,rawtypes
			return (SqmNumericExpression) this;
		}
		else {
			return new SqmNumericExpressionWrapper<>( asBigInteger() );
		}
	}

}

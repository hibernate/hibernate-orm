/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanExpressionImplementor
		extends SqmComparableExpressionImplementor<Boolean>, SqmBooleanExpression {
	SqmCriteriaNodeBuilder nodeBuilder();

	@Nonnull
	@Override
	default SqmComparableExpression<Boolean> max() {
		throw new UnsupportedOperationException( "max() not supported" );
	}

	@Nonnull
	@Override
	default SqmComparableExpression<Boolean> min() {
		throw new UnsupportedOperationException( "min() not supported" );
	}

	@Nonnull
	@Override
	default SqmPredicate and(@Nonnull Expression<Boolean> y) {
		return nodeBuilder().and( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate or(@Nonnull Expression<Boolean> y) {
		return nodeBuilder().or( this, y );
	}

	@Nonnull
	@Override
	default SqmPredicate not() {
		return nodeBuilder().not( this );
	}

	@Nonnull
	@Override
	default SqmBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmBooleanExpression coalesce(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	default SqmBooleanExpression nullif(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}
}

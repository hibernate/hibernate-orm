/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanExpressionImplementor
		extends SqmComparableExpressionImplementor<Boolean>, SqmBooleanExpression {
	SqmCriteriaNodeBuilder nodeBuilder();

	@Override
	default SqmComparableExpression<Boolean> max() {
		throw new UnsupportedOperationException( "max() not supported" );
	}

	@Override
	default SqmComparableExpression<Boolean> min() {
		throw new UnsupportedOperationException( "min() not supported" );
	}

	@Override
	default SqmPredicate and(Expression<Boolean> y) {
		return nodeBuilder().and( this, y );
	}

	@Override
	default SqmPredicate or(Expression<Boolean> y) {
		return nodeBuilder().or( this, y );
	}

	@Override
	default SqmPredicate not() {
		return nodeBuilder().not( this );
	}

	@Override
	default SqmBooleanExpression coalesce(Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	default SqmBooleanExpression coalesce(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	default SqmBooleanExpression nullif(Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Override
	default SqmBooleanExpression nullif(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}
}

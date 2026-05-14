/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.time.temporal.Temporal;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.TemporalField;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;

/**
 * @author Steve Ebersole
 */
public interface SqmTemporalExpressionImplementor<T extends Temporal & Comparable<? super T>>
		extends SqmComparableExpressionImplementor<T>, SqmTemporalExpression<T> {
	SqmCriteriaNodeBuilder nodeBuilder();

	@Override
	default <N extends Number & Comparable<N>> SqmNumericExpression<N> extract(TemporalField<N, T> field) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().extract( field, this ) );
	}

	@Override
	default SqmTemporalExpression<T> coalesce(Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	default SqmTemporalExpression<T> coalesce(T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	default SqmTemporalExpression<T> nullif(Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Override
	default SqmTemporalExpression<T> nullif(T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

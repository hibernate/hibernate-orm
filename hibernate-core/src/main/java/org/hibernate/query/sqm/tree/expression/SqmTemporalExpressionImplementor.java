/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.time.temporal.Temporal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.TemporalField;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;

/**
 * @author Steve Ebersole
 */
public interface SqmTemporalExpressionImplementor<T extends Temporal & Comparable<? super T>>
		extends SqmComparableExpressionImplementor<T>, SqmTemporalExpression<T> {
	SqmCriteriaNodeBuilder nodeBuilder();

	@Nonnull
	@Override
	default <N extends Number & Comparable<N>> SqmNumericExpression<N> extract(@Nonnull TemporalField<N, T> field) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().extract( field, this ) );
	}

	@Nonnull
	@Override
	default SqmTemporalExpression<T> coalesce(@Nonnull Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTemporalExpression<T> coalesce(T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTemporalExpression<T> nullif(@Nonnull Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	default SqmTemporalExpression<T> nullif(T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

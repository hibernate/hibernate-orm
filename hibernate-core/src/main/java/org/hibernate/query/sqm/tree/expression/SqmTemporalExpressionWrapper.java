/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.time.temporal.Temporal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmTemporalExpressionWrapper<T extends Temporal & Comparable<? super T>>
		extends SqmComparableExpressionWrapper<T>
		implements SqmTemporalExpressionImplementor<T> {
	public SqmTemporalExpressionWrapper(SqmExpression<T> wrappedExpression) {
		super( wrappedExpression );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> coalesce(@Nonnull Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> coalesce(T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> nullif(@Nonnull Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> nullif(T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaTemporalExpression;

import java.time.temporal.Temporal;

/**
 * @author Steve Ebersole
 */
public interface SqmTemporalExpression<T extends Temporal & Comparable<? super T>>
		extends SqmComparableExpression<T>, JpaTemporalExpression<T> {
	@Nonnull
	@Override
	SqmTemporalExpression<T> coalesce(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	SqmTemporalExpression<T> coalesce(T y);

	@Nonnull
	@Override
	SqmTemporalExpression<T> nullif(@Nonnull Expression<? extends T> y);

	@Nonnull
	@Override
	SqmTemporalExpression<T> nullif(T y);
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaTemporalExpression;

import java.time.temporal.Temporal;

/**
 * @author Steve Ebersole
 */
public interface SqmTemporalExpression<T extends Temporal & Comparable<? super T>> extends SqmExpression<T>, JpaTemporalExpression<T> {
	@Override
	SqmTemporalExpression<T> coalesce(Expression<? extends T> y);

	@Override
	SqmTemporalExpression<T> coalesce(T y);

	@Override
	SqmTemporalExpression<T> nullif(Expression<? extends T> y);

	@Override
	SqmTemporalExpression<T> nullif(T y);
}

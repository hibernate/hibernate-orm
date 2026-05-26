/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaSimpleCase<C,R> extends JpaExpression<R>, CriteriaBuilder.SimpleCase<C,R> {
	@Nonnull
	@Override
	JpaExpression<C> getExpression();

	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(C condition, R result);

	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(C condition, @Nonnull Expression<? extends R> result);

	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nonnull Expression<? extends C> condition, R result);

	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nonnull Expression<? extends C> condition, @Nonnull Expression<? extends R> result);

	@Nonnull
	@Override
	JpaSimpleCase<C,R> otherwise(R result);

	@Nonnull
	@Override
	JpaSimpleCase<C,R> otherwise(@Nonnull Expression<? extends R> result);
}

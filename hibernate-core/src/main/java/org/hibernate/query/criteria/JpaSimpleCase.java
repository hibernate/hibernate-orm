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
	/**
	 * Return the expression tested by this simple case expression.
	 */
	@Nonnull
	@Override
	JpaExpression<C> getExpression();

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(C condition, R result);

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(C condition, @Nonnull Expression<? extends R> result);

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nonnull Expression<? extends C> condition, R result);

	/**
	 * Add a when-then clause to this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C, R> when(@Nonnull Expression<? extends C> condition, @Nonnull Expression<? extends R> result);

	/**
	 * Set the otherwise result of this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C,R> otherwise(R result);

	/**
	 * Set the otherwise result of this simple case expression.
	 */
	@Nonnull
	@Override
	JpaSimpleCase<C,R> otherwise(@Nonnull Expression<? extends R> result);
}

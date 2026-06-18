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
public interface JpaSearchedCase<T> extends JpaExpression<T>, CriteriaBuilder.Case<T> {
	/**
	 * Add a when-then clause to this searched case expression.
	 */
	@Nonnull
	@Override
	JpaSearchedCase<T> when(@Nonnull Expression<Boolean> condition, T result);

	/**
	 * Add a when-then clause to this searched case expression.
	 */
	@Nonnull
	@Override
	JpaSearchedCase<T> when(@Nonnull Expression<Boolean> condition, @Nonnull Expression<? extends T> result);

	/**
	 * Set the otherwise result of this searched case expression.
	 */
	@Nonnull
	@Override
	JpaExpression<T> otherwise(T result);

	/**
	 * Set the otherwise result of this searched case expression.
	 */
	@Nonnull
	@Override
	JpaExpression<T> otherwise(@Nonnull Expression<? extends T> result);
}

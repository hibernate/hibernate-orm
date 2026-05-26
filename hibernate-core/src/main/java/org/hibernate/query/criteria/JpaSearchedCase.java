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
	@Nonnull
	@Override
	JpaSearchedCase<T> when(@Nonnull Expression<Boolean> condition, T result);

	@Nonnull
	@Override
	JpaSearchedCase<T> when(@Nonnull Expression<Boolean> condition, @Nonnull Expression<? extends T> result);

	@Nonnull
	@Override
	JpaExpression<T> otherwise(T result);

	@Nonnull
	@Override
	JpaExpression<T> otherwise(@Nonnull Expression<? extends T> result);
}

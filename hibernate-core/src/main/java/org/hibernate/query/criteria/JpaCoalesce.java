/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaCoalesce<T> extends JpaExpression<T>, CriteriaBuilder.Coalesce<T> {
	/**
	 * Add a value to this coalesce expression.
	 */
	@Nonnull
	@Override
	JpaCoalesce<T> value(@Nullable T value);

	/**
	 * Add a value to this coalesce expression.
	 */
	@Nonnull
	@Override
	JpaCoalesce<T> value(@Nonnull Expression<? extends T> value);

	/**
	 * Add a value to this coalesce expression.
	 */
	@Nonnull
	JpaCoalesce<T> value(@Nonnull JpaExpression<? extends T> value);

	/**
	 * Add values to this coalesce expression.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	JpaCoalesce<T> values(T... values);
}

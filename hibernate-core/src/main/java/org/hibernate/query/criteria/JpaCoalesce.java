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
	@Nonnull
	@Override
	JpaCoalesce<T> value(@Nullable T value);

	@Nonnull
	@Override
	JpaCoalesce<T> value(@Nonnull Expression<? extends T> value);

	@Nonnull
	JpaCoalesce<T> value(@Nonnull JpaExpression<? extends T> value);

	@Nonnull
	@SuppressWarnings("unchecked")
	JpaCoalesce<T> values(T... values);
}

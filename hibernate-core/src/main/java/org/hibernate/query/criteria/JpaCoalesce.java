/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public interface JpaCoalesce<T> extends JpaExpression<T>, CriteriaBuilder.Coalesce<T> {
	@Override
	JpaCoalesce<T> value(@Nullable T value);

	@Override
	JpaCoalesce<T> value(Expression<? extends T> value);

	JpaCoalesce<T> value(JpaExpression<? extends T> value);

	@SuppressWarnings("unchecked")
	JpaCoalesce<T> values(T... values);
}

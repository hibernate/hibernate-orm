/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public interface JpaSearchedCase<T> extends JpaExpression<T>, CriteriaBuilder.Case<T> {
	@Override
	JpaSearchedCase<T> when(Expression<Boolean> condition, T result);

	@Override
	JpaSearchedCase<T> when(Expression<Boolean> condition, Expression<? extends T> result);

	@Override
	JpaExpression<T> otherwise(T result);

	@Override
	JpaExpression<T> otherwise(Expression<? extends T> result);
}

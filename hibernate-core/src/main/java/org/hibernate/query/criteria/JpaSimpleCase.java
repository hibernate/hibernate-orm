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
public interface JpaSimpleCase<C,R> extends JpaExpression<R>, CriteriaBuilder.SimpleCase<C,R> {
	@Override
	JpaExpression<C> getExpression();

	@Override
	JpaSimpleCase<C, R> when(C condition, R result);

	@Override
	JpaSimpleCase<C, R> when(C condition, Expression<? extends R> result);

	@Override
	JpaSimpleCase<C, R> when(Expression<? extends C> condition, R result);

	@Override
	JpaSimpleCase<C, R> when(Expression<? extends C> condition, Expression<? extends R> result);

	@Override
	JpaSimpleCase<C,R> otherwise(R result);

	@Override
	JpaSimpleCase<C,R> otherwise(Expression<? extends R> result);
}

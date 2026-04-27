/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaBooleanExpression;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanExpression extends SqmComparableExpression<Boolean>, JpaBooleanExpression {
	@Override
	SqmBooleanExpression coalesce(Boolean y);

	@Override
	SqmBooleanExpression coalesce(Expression<? extends Boolean> y);

	@Override
	SqmBooleanExpression nullif(Boolean y);

	@Override
	SqmBooleanExpression nullif(Expression<? extends Boolean> y);

	@Override
	SqmPredicate and(Expression<Boolean> y);

	@Override
	SqmPredicate or(Expression<Boolean> y);

	@Override
	SqmPredicate not();


}

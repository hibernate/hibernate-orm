/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanPath extends SqmPath<Boolean>, SqmBooleanExpression {
	@Override
	SqmBooleanExpression coalesce(Expression<? extends Boolean> y);

	@Override
	SqmBooleanExpression coalesce(Boolean y);

	@Override
	SqmBooleanExpression nullif(Expression<? extends Boolean> y);

	@Override
	SqmBooleanExpression nullif(Boolean y);
}

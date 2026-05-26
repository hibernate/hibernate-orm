/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanPath extends SqmPath<Boolean>, SqmBooleanExpression {
	@Nonnull
	@Override
	SqmBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	SqmBooleanExpression coalesce(Boolean y);

	@Nonnull
	@Override
	SqmBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	SqmBooleanExpression nullif(Boolean y);
}

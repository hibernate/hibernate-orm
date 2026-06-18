/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaBooleanExpression;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface SqmBooleanExpression extends SqmComparableExpression<Boolean>, JpaBooleanExpression {
	@Nonnull
	@Override
	SqmBooleanExpression coalesce(@Nonnull Boolean y);

	@Nonnull
	@Override
	SqmBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	SqmBooleanExpression nullif(Boolean y);

	@Nonnull
	@Override
	SqmBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y);

	@Nonnull
	@Override
	SqmPredicate and(@Nonnull Expression<Boolean> y);

	@Nonnull
	@Override
	SqmPredicate or(@Nonnull Expression<Boolean> y);

	@Nonnull
	@Override
	SqmPredicate not();


}

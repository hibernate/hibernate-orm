/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.criteria.JpaTextExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmTextExpression extends SqmComparableExpression<String>, JpaTextExpression {
	@Nonnull
	@Override
	SqmTextExpression coalesce(@Nonnull Expression<? extends String> y);

	@Nonnull
	@Override
	SqmTextExpression coalesce(String y);

	@Nonnull
	@Override
	SqmTextExpression nullif(@Nonnull Expression<? extends String> y);

	@Nonnull
	@Override
	SqmTextExpression nullif(String y);
}

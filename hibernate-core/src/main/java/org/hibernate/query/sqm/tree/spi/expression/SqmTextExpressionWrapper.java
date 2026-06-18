/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmTextExpressionWrapper
		extends SqmComparableExpressionWrapper<String>
		implements SqmTextExpressionImplementor {
	public SqmTextExpressionWrapper(SqmExpression<String> wrappedExpression) {
		super( wrappedExpression );
	}

	@Nonnull
	@Override
	public SqmTextExpression coalesce(@Nonnull Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTextExpression coalesce(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTextExpression nullif(@Nonnull Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTextExpression nullif(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}
}

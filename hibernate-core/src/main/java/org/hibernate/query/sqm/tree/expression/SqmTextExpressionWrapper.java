/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

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

	@Override
	public SqmTextExpression coalesce(Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmTextExpression coalesce(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmTextExpression nullif(Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmTextExpression nullif(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}
}

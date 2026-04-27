/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmBooleanExpressionWrapper
		extends AbstractSqmExpression<Boolean>
		implements SqmBooleanExpressionImplementor, SqmExpressionWrapper<Boolean> {
	private final SqmExpression<Boolean> wrappedExpression;

	public SqmBooleanExpressionWrapper(SqmExpression<Boolean> wrappedExpression) {
		super( wrappedExpression.getNodeType(), wrappedExpression.nodeBuilder() );
		this.wrappedExpression = wrappedExpression;
	}

	@Override
	public SqmExpression<Boolean> getWrappedExpression() {
		return wrappedExpression;
	}

	@Override
	public SqmBooleanExpression coalesce(Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmBooleanExpression coalesce(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmBooleanExpression nullif(Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmBooleanExpression nullif(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmBooleanExpression max() {
		throw new UnsupportedOperationException( "Boolean expression does not support max()" );
	}

	@Override
	public SqmBooleanExpression min() {
		throw new UnsupportedOperationException( "Boolean expression does not support min()" );
	}

	@Override
	public SqmExpression<Boolean> copy(SqmCopyContext context) {
		return wrappedExpression.copy( context );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		wrappedExpression.appendHqlString( hql, context );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return wrappedExpression.accept( walker );
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmBooleanExpressionWrapper that
			&& getClass() == that.getClass()
			&& wrappedExpression.equals( that.wrappedExpression );
	}

	@Override
	public int hashCode() {
		return wrappedExpression.hashCode();
	}
}

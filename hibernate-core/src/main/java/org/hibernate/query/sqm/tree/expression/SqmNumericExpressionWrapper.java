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
public class SqmNumericExpressionWrapper<N extends Number & Comparable<N>>
		extends AbstractSqmExpression<N>
		implements SqmNumericExpressionImplementor<N>, SqmExpressionWrapper<N> {
	private final SqmExpression<N> wrappedExpression;

	public SqmNumericExpressionWrapper(SqmExpression<N> wrappedExpression) {
		super( wrappedExpression.getNodeType(), wrappedExpression.nodeBuilder() );
		this.wrappedExpression = wrappedExpression;
	}

	@Override
	public SqmExpression<N> getWrappedExpression() {
		return wrappedExpression;
	}

	@Override
	public SqmNumericExpression<N> coalesce(Expression<? extends N> y) {
		var expr = nodeBuilder().coalesce( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Override
	public SqmNumericExpression<N> coalesce(N y) {
		var expr = nodeBuilder().coalesce( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Override
	public SqmNumericExpression<N> nullif(Expression<? extends N> y) {
		var expr = nodeBuilder().nullif( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Override
	public SqmNumericExpression<N> nullif(N y) {
		var expr = nodeBuilder().nullif( this, y);
		return new SqmNumericExpressionWrapper<>( expr );
	}

	@Override
	public SqmExpression<N> copy(SqmCopyContext context) {
		return wrappedExpression.copy( context );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return wrappedExpression.accept( walker );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		wrappedExpression.appendHqlString( hql, context );
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
		return object instanceof SqmNumericExpressionWrapper<?> that
			&& getClass() == that.getClass()
			&& wrappedExpression.equals( that.wrappedExpression );
	}

	@Override
	public int hashCode() {
		return wrappedExpression.hashCode();
	}
}

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
public class SqmComparableExpressionWrapper<C extends Comparable<? super C>>
		extends AbstractSqmExpression<C>
		implements SqmComparableExpressionImplementor<C>, SqmExpressionWrapper<C> {
	private final SqmExpression<C> wrappedExpression;

	public SqmComparableExpressionWrapper(SqmExpression<C> wrappedExpression) {
		super( wrappedExpression.getNodeType(), wrappedExpression.nodeBuilder() );
		this.wrappedExpression = wrappedExpression;
	}

	@Override
	public SqmExpression<C> getWrappedExpression() {
		return wrappedExpression;
	}

	@Override
	public SqmComparableExpression<C> coalesce(Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmComparableExpression<C> coalesce(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmComparableExpression<C> nullif(Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmComparableExpression<C> nullif(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmExpression<C> copy(SqmCopyContext context) {
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
		return object instanceof SqmComparableExpressionWrapper<?> that
			&& getClass() == that.getClass()
			&& wrappedExpression.equals( that.wrappedExpression );
	}

	@Override
	public int hashCode() {
		return wrappedExpression.hashCode();
	}
}

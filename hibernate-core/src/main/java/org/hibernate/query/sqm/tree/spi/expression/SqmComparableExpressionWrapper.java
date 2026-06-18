/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

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

	@Nonnull
	@Override
	public SqmComparableExpression<C> coalesce(@Nonnull Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmComparableExpression<C> coalesce(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmComparableExpression<C> nullif(@Nonnull Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
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

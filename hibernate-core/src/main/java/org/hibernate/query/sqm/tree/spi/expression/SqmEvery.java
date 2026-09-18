/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;

import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;

import jakarta.annotation.Nullable;

/**
 * @author Gavin King
 */
public class SqmEvery<T> extends AbstractSqmExpression<T> {

	private final SqmSubQuery<T> subquery;

	public SqmEvery(@Nonnull SqmSubQuery<T> subquery, @Nonnull NodeBuilder criteriaBuilder) {
		super( subquery.getNodeType(), criteriaBuilder );
		this.subquery = subquery;
	}

	@Override
	public @Nullable SqmBindableType<T> getNodeType() {
		return subquery.getNodeType();
	}

	@Override
	public @Nullable Integer getTupleLength() {
		return subquery.getTupleLength();
	}

	@Nonnull
	@Override
	public SqmEvery<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEvery<T> expression = context.registerCopy(
				this,
				new SqmEvery<>( subquery.copy( context ), nodeBuilder() )
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	public SqmSubQuery<T> getSubquery() {
		return subquery;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitEvery( this );
	}
	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "all " );
		subquery.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmEvery<?> sqmAny
			&& this.subquery.equals( sqmAny.subquery );
	}

	@Override
	public int hashCode() {
		return subquery.hashCode();
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmEvery<?> sqmAny
			&& this.subquery.isCompatible( sqmAny.subquery );
	}

	@Override
	public int cacheHashCode() {
		return subquery.cacheHashCode();
	}
}

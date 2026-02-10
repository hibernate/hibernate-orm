/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmSelfRenderingExpression<T> extends AbstractSqmExpression<T> {
	private final Function<SemanticQueryWalker<?>, Expression> renderer;

	public SqmSelfRenderingExpression(
			Function<SemanticQueryWalker<?>, Expression> renderer,
			@Nullable SqmBindableType<T> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.renderer = renderer;
	}

	@Override
	public SqmSelfRenderingExpression<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmSelfRenderingExpression<T> expression = context.registerCopy(
				this,
				new SqmSelfRenderingExpression<>( renderer, getNodeType(), nodeBuilder() )
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		//noinspection unchecked
		return (X) renderer.apply( walker );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		throw new UnsupportedOperationException();
	}

	// No equals() / hashCode() because this stuff is only
	// ever used internally and is irrelevant for caching,
	// so basing equality on the object identity is fine

	@Override
	public boolean isCompatible(Object object) {
		return this == object;
	}

	@Override
	public int cacheHashCode() {
		return System.identityHashCode( this );
	}
}

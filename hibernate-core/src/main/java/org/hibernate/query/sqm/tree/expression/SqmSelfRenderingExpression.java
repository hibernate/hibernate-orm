/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Function;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
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
			SqmExpressible<T> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.renderer = renderer;
	}

	@Override
	public SqmSelfRenderingExpression<T> copy(SqmCopyContext context) {
		final SqmSelfRenderingExpression<T> existing = context.getCopy( this );
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

	//TODO: what is a correct impl of equals() / hashCode() here?
}

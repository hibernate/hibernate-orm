/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;


/**
 * @author Gavin King
 */
public class SqmDistinct<T> extends AbstractSqmNode implements SqmTypedNode<T> {

	private final SqmExpression<T> expression;

	public SqmDistinct(SqmExpression<T> expression, NodeBuilder builder) {
		super( builder );
		this.expression = expression;
	}

	@Override
	public SqmDistinct<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmDistinct<>(
						expression.copy( context ),
						nodeBuilder()
				)
		);
	}

	public SqmExpression<T> getExpression() {
		return expression;
	}

	@Override
	public @Nullable SqmBindableType<T> getNodeType() {
		return expression.getNodeType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDistinct(this);
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "distinct " );
		expression.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmDistinct<?> that
			&& expression.equals( that.expression );
	}

	@Override
	public int hashCode() {
		return expression.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmDistinct<?> that
			&& expression.isCompatible( that.expression );
	}

	@Override
	public int cacheHashCode() {
		return expression.cacheHashCode();
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import java.util.Objects;

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
		final SqmDistinct<T> existing = context.getCopy( this );
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
	public SqmExpressible<T> getNodeType() {
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
	public boolean equals(Object object) {
		return object instanceof SqmDistinct<?> that
			&& Objects.equals( this.expression, that.expression );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( expression );
	}
}

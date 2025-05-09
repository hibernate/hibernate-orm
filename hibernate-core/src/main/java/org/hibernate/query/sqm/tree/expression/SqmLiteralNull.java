/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull<T> extends SqmLiteral<T> {

	private static final SqmExpressible<Object> NULL_TYPE = NullSqmExpressible.NULL_SQM_EXPRESSIBLE;

	public SqmLiteralNull(NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this( (SqmExpressible<T>) NULL_TYPE, nodeBuilder );
	}

	public SqmLiteralNull(SqmExpressible<T> expressibleType, NodeBuilder nodeBuilder) {
		super( expressibleType, nodeBuilder );
	}

	@Override
	public SqmLiteralNull<T> copy(SqmCopyContext context) {
		final SqmLiteralNull<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralNull<T> expression = context.registerCopy(
				this,
				new SqmLiteralNull<>(
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitLiteral( this );
	}

	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "null" );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmLiteralNull;
	}

	@Override
	public int hashCode() {
		return 1;
	}
}

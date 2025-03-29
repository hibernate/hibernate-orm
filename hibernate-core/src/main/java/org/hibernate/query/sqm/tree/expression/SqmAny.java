/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Gavin King
 */
public class SqmAny<T> extends AbstractSqmExpression<T> {

	private final SqmSubQuery<T> subquery;

	public SqmAny(SqmSubQuery<T> subquery, NodeBuilder criteriaBuilder) {
		super( subquery.getNodeType(), criteriaBuilder );
		this.subquery = subquery;
	}

	@Override
	public @Nullable SqmExpressible<T> getNodeType() {
		return subquery.getNodeType();
	}

	@Override
	public Integer getTupleLength() {
		return subquery.getTupleLength();
	}

	@Override
	public SqmAny<T> copy(SqmCopyContext context) {
		final SqmAny<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAny<T> expression = context.registerCopy(
				this,
				new SqmAny<>(
						subquery.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmSubQuery<T> getSubquery() {
		return subquery;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAny( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		hql.append( "any " );
		subquery.appendHqlString( hql );
	}

}

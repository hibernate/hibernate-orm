/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * @author Gavin King
 */
public class SqmEvery<T> extends AbstractSqmExpression<T> {

	private final SqmSubQuery<T> subquery;

	public SqmEvery(SqmSubQuery<T> subquery, NodeBuilder criteriaBuilder) {
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
	public SqmEvery<T> copy(SqmCopyContext context) {
		final SqmEvery<T> existing = context.getCopy( this );
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

	public SqmSubQuery<T> getSubquery() {
		return subquery;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEvery( this );
	}
	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "all " );
		subquery.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmEvery<?> sqmAny
			&& Objects.equals( this.subquery, sqmAny.subquery );
	}

	@Override
	public int hashCode() {
		return subquery.hashCode();
	}
}

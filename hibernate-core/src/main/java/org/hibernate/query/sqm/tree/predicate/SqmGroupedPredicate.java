/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmGroupedPredicate extends AbstractSqmPredicate {
	private final SqmPredicate subPredicate;

	public SqmGroupedPredicate(SqmPredicate subPredicate, NodeBuilder nodeBuilder) {
		super( subPredicate.getExpressible(), nodeBuilder );
		this.subPredicate = subPredicate;
	}

	@Override
	public SqmGroupedPredicate copy(SqmCopyContext context) {
		final SqmGroupedPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmGroupedPredicate predicate = context.registerCopy(
				this,
				new SqmGroupedPredicate(
						subPredicate.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmPredicate getSubPredicate() {
		return subPredicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitGroupedPredicate( this );
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		final List<Expression<Boolean>> expressions = new ArrayList<>( 1 );
		expressions.add( subPredicate );
		return expressions;
	}

	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( '(' );
		subPredicate.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmGroupedPredicate that
			&& Objects.equals( subPredicate, that.subPredicate );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( subPredicate );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmNegatedPredicate extends AbstractSqmPredicate {
	private final SqmPredicate wrappedPredicate;

	public SqmNegatedPredicate(SqmPredicate wrappedPredicate, NodeBuilder nodeBuilder) {
		super( nodeBuilder.getBooleanType(), nodeBuilder );
		this.wrappedPredicate = wrappedPredicate;
	}

	@Override
	public SqmNegatedPredicate copy(SqmCopyContext context) {
		final SqmNegatedPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNegatedPredicate predicate = context.registerCopy(
				this,
				new SqmNegatedPredicate(
						wrappedPredicate.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmPredicate getWrappedPredicate() {
		return wrappedPredicate;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		final List<Expression<Boolean>> expressions = new ArrayList<>( 1 );
		expressions.add( wrappedPredicate );
		return expressions;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitNegatedPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "not (" );
		wrappedPredicate.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmNegatedPredicate that
			&& wrappedPredicate.equals( that.wrappedPredicate );
	}

	@Override
	public int hashCode() {
		return wrappedPredicate.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmNegatedPredicate that
			&& wrappedPredicate.isCompatible( that.wrappedPredicate );
	}

	@Override
	public int cacheHashCode() {
		return wrappedPredicate.cacheHashCode();
	}

	@Override
	public boolean isNegated() {
		return true;
	}

	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}
}

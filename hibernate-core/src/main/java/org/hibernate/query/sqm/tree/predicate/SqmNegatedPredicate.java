/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmNegatedPredicate extends AbstractNegatableSqmPredicate {
	private final SqmPredicate wrappedPredicate;

	public SqmNegatedPredicate(SqmPredicate wrappedPredicate, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.wrappedPredicate = wrappedPredicate;
	}

	public SqmNegatedPredicate(
			SqmPredicate wrappedPredicate,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
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
						isNegated(),
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
	public void appendHqlString(StringBuilder hql) {
		hql.append( "not (" );
		wrappedPredicate.appendHqlString( hql );
		hql.append( ')' );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

}

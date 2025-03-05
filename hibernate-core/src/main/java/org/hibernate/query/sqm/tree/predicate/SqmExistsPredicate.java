/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Gavin King
 */
public class SqmExistsPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;

	public SqmExistsPredicate(
			SqmExpression<?> expression,
			NodeBuilder nodeBuilder) {
		this( expression, false, nodeBuilder );
	}

	public SqmExistsPredicate(
			SqmExpression<?> expression,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;

		expression.applyInferableType( expression.getNodeType() );
	}

	@Override
	public SqmExistsPredicate copy(SqmCopyContext context) {
		final SqmExistsPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmExistsPredicate predicate = context.registerCopy(
				this,
				new SqmExistsPredicate(
						expression.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitExistsPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		if ( isNegated() ) {
			hql.append( "not exists " );
		}
		else {
			hql.append( "exists " );
		}
		expression.appendHqlString( hql );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmExistsPredicate( expression, !isNegated(), nodeBuilder() );
	}
}

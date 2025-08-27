/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmNullnessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;

	public SqmNullnessPredicate(SqmExpression<?> expression, NodeBuilder nodeBuilder) {
		this( expression, false, nodeBuilder );
	}

	public SqmNullnessPredicate(SqmExpression<?> expression, boolean negated, NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
	}

	@Override
	public SqmNullnessPredicate copy(SqmCopyContext context) {
		final SqmNullnessPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNullnessPredicate predicate = context.registerCopy(
				this,
				new SqmNullnessPredicate(
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
		return walker.visitIsNullPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " is not null" );
		}
		else {
			hql.append( " is null" );
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmNullnessPredicate that
			&& this.isNegated() == that.isNegated()
			&& Objects.equals( this.expression, that.expression );
	}

	@Override
	public int hashCode() {
		return Objects.hash( isNegated(), expression );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmNullnessPredicate( expression, !isNegated(), nodeBuilder() );
	}
}

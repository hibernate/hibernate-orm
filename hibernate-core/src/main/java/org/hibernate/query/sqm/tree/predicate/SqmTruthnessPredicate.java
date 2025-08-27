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
 * @author Gavin King
 */
public class SqmTruthnessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;
	private final boolean value;

	public SqmTruthnessPredicate(SqmExpression<?> expression, boolean value, boolean negated, NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
		this.value = value;
	}

	public boolean getBooleanValue() {
		return value;
	}

	@Override
	public SqmTruthnessPredicate copy(SqmCopyContext context) {
		final SqmTruthnessPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTruthnessPredicate predicate = context.registerCopy(
				this,
				new SqmTruthnessPredicate(
						expression.copy( context ),
						getBooleanValue(),
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
		return walker.visitIsTruePredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		hql.append(" is ");
		if ( isNegated() ) {
			hql.append( "not " );
		}
		hql.append( getBooleanValue() );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTruthnessPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.value == that.value
			&& Objects.equals( this.expression, that.expression );
	}

	@Override
	public int hashCode() {
		return Objects.hash( isNegated(), value, expression );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmTruthnessPredicate( expression, getBooleanValue(), !isNegated(), nodeBuilder() );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.Objects;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

/**
 * @author Steve Ebersole
 */
public class SqmBetweenPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;
	private final SqmExpression<?> lowerBound;
	private final SqmExpression<?> upperBound;

	public SqmBetweenPredicate(
			SqmExpression<?> expression,
			SqmExpression<?> lowerBound,
			SqmExpression<?> upperBound,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;

		assertComparable( expression, lowerBound, nodeBuilder );
		assertComparable( expression, upperBound, nodeBuilder );

		final SqmExpressible<?> expressibleType = QueryHelper.highestPrecedenceType(
				expression.getExpressible(),
				lowerBound.getExpressible(),
				upperBound.getExpressible()
		);

		expression.applyInferableType( expressibleType );
		lowerBound.applyInferableType( expressibleType );
		upperBound.applyInferableType( expressibleType );
	}

	@Override
	public SqmBetweenPredicate copy(SqmCopyContext context) {
		final SqmBetweenPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmBetweenPredicate predicate = context.registerCopy(
				this,
				new SqmBetweenPredicate(
						expression.copy( context ),
						lowerBound.copy( context ),
						upperBound.copy( context ),
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

	public SqmExpression<?> getLowerBound() {
		return lowerBound;
	}

	public SqmExpression<?> getUpperBound() {
		return upperBound;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBetweenPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " between " );
		lowerBound.appendHqlString( hql, context );
		hql.append( " and " );
		upperBound.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmBetweenPredicate that
			&& this.isNegated() == that.isNegated()
			&& Objects.equals( expression, that.expression )
			&& Objects.equals( lowerBound, that.lowerBound )
			&& Objects.equals( upperBound, that.upperBound );
	}

	@Override
	public int hashCode() {
		return Objects.hash( isNegated(), expression, lowerBound, upperBound );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmBetweenPredicate( expression, lowerBound, upperBound, ! isNegated(), nodeBuilder() );
	}
}

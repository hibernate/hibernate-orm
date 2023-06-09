/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

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
	public void appendHqlString(StringBuilder sb) {
		expression.appendHqlString( sb );
		if ( isNegated() ) {
			sb.append( " not" );
		}
		sb.append( " between " );
		lowerBound.appendHqlString( sb );
		sb.append( " and " );
		upperBound.appendHqlString( sb );
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmBetweenPredicate( expression, lowerBound, upperBound, ! isNegated(), nodeBuilder() );
	}
}

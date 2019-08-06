/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
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

		final SqmExpressable<?> expressableType = QueryHelper.highestPrecedenceType(
				expression.getNodeType(),
				lowerBound.getNodeType(),
				upperBound.getNodeType()
		);

		expression.applyInferableType( expressableType );
		lowerBound.applyInferableType( expressableType );
		upperBound.applyInferableType( expressableType );
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
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmComparisonPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> leftHandExpression;
	private ComparisonOperator operator;
	private final SqmExpression<?> rightHandExpression;

	public SqmComparisonPredicate(
			SqmExpression<?> leftHandExpression,
			ComparisonOperator operator,
			SqmExpression<?> rightHandExpression,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.leftHandExpression = leftHandExpression;
		this.rightHandExpression = rightHandExpression;
		this.operator = operator;

		final ExpressableType<?> expressableType = QueryHelper.highestPrecedenceType(
				leftHandExpression.getNodeType(),
				rightHandExpression.getNodeType()
		);

		leftHandExpression.applyInferableType( expressableType );
		rightHandExpression.applyInferableType( expressableType );
	}

	public SqmExpression<?> getLeftHandExpression() {
		return leftHandExpression;
	}

	public SqmExpression<?> getRightHandExpression() {
		return rightHandExpression;
	}

	public ComparisonOperator getSqmOperator() {
		return operator;
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public void negate() {
		this.operator = this.operator.negated();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitComparisonPredicate( this );
	}
}

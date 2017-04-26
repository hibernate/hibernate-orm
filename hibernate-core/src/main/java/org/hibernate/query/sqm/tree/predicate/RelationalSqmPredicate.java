/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class RelationalSqmPredicate implements SqmPredicate, NegatableSqmPredicate {

	private final SqmExpression leftHandExpression;
	private final SqmExpression rightHandExpression;
	private RelationalPredicateOperator operator;

	public RelationalSqmPredicate(
			RelationalPredicateOperator operator,
			SqmExpression leftHandExpression,
			SqmExpression rightHandExpression) {
		this.leftHandExpression = leftHandExpression;
		this.rightHandExpression = rightHandExpression;
		this.operator = operator;
	}

	public SqmExpression getLeftHandExpression() {
		return leftHandExpression;
	}

	public SqmExpression getRightHandExpression() {
		return rightHandExpression;
	}

	public RelationalPredicateOperator getOperator() {
		return operator;
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public void negate() {
		this.operator = this.operator.negate();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitRelationalPredicate( this );
	}
}

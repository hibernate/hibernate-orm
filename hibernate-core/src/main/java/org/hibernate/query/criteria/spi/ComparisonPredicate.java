/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.spi.ComparisonOperator;

/**
 * Models a basic relational comparison predicate.
 *
 * @author Steve Ebersole
 */
public class ComparisonPredicate extends AbstractSimplePredicate {
	private final ExpressionImplementor<?> leftHandSide;
	private final ComparisonOperator comparisonOperator;
	private final ExpressionImplementor<?> rightHandSide;

	public ComparisonPredicate(
			ExpressionImplementor<?> leftHandSide,
			ComparisonOperator comparisonOperator,
			ExpressionImplementor<?> rightHandSide,
			CriteriaNodeBuilder builder) {
		super( builder );
		this.leftHandSide = leftHandSide;
		this.comparisonOperator = comparisonOperator;
		this.rightHandSide = rightHandSide;
	}

	public ComparisonOperator getComparisonOperator() {
		return comparisonOperator;
	}

	public ExpressionImplementor getLeftHandOperand() {
		return leftHandSide;
	}

	public ExpressionImplementor getRightHandOperand() {
		return rightHandSide;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitComparisonPredicate( this );
	}

}

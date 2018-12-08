/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

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
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitComparisonPredicate( this );
	}

	/**
	 * Defines the comparison operators.  We could also get away with
	 * only 3 and use negation...
	 */
	public enum ComparisonOperator {
		EQUAL {
			public ComparisonOperator negated() {
				return NOT_EQUAL;
			}
		},
		NOT_EQUAL {
			public ComparisonOperator negated() {
				return EQUAL;
			}
		},
		LESS_THAN {
			public ComparisonOperator negated() {
				return GREATER_THAN_OR_EQUAL;
			}
		},
		LESS_THAN_OR_EQUAL {
			public ComparisonOperator negated() {
				return GREATER_THAN;
			}
		},
		GREATER_THAN {
			public ComparisonOperator negated() {
				return LESS_THAN_OR_EQUAL;
			}
		},
		GREATER_THAN_OR_EQUAL {
			public ComparisonOperator negated() {
				return LESS_THAN;
			}
		};

		public abstract ComparisonOperator negated();
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.function.Supplier;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.InferableTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

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

		if ( rightHandExpression instanceof InferableTypeSqmExpression ) {
			final Supplier<? extends ExpressableType> inference = leftHandExpression.getInferableType();
			if ( inference != null ) {
				( (InferableTypeSqmExpression) rightHandExpression ).impliedType( inference );
			}
		}

		if ( leftHandExpression instanceof InferableTypeSqmExpression ) {
			final Supplier<? extends ExpressableType> inference = rightHandExpression.getInferableType();
			if ( inference != null ) {
				( (InferableTypeSqmExpression) leftHandExpression ).impliedType( rightHandExpression.getInferableType() );
			}
		}
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

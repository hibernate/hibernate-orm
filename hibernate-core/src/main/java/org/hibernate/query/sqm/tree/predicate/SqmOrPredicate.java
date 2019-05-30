/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Arrays;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmOrPredicate extends AbstractSqmExpression<Boolean> implements SqmJunctivePredicate {
	private final SqmPredicate leftHandPredicate;
	private final SqmPredicate rightHandPredicate;

	public SqmOrPredicate(
			SqmPredicate leftHandPredicate,
			SqmPredicate rightHandPredicate,
			NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.leftHandPredicate = leftHandPredicate;
		this.rightHandPredicate = rightHandPredicate;
	}

	@Override
	public SqmPredicate getLeftHandPredicate() {
		return leftHandPredicate;
	}

	@Override
	public SqmPredicate getRightHandPredicate() {
		return rightHandPredicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitOrPredicate( this );
	}

	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

	@Override
	public BooleanOperator getOperator() {
		return BooleanOperator.OR;
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return Arrays.asList( leftHandPredicate, rightHandPredicate );
	}
}

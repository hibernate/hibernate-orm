/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

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
		this( leftHandExpression, operator, rightHandExpression, false, nodeBuilder );
	}

	private SqmComparisonPredicate(
			SqmExpression<?> leftHandExpression,
			ComparisonOperator operator,
			SqmExpression<?> rightHandExpression,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.leftHandExpression = leftHandExpression;
		this.rightHandExpression = rightHandExpression;
		this.operator = operator;

		assertComparable( leftHandExpression, rightHandExpression, nodeBuilder );

		final SqmExpressible<?> expressibleType = QueryHelper.highestPrecedenceType(
				leftHandExpression.getExpressible(),
				rightHandExpression.getExpressible()
		);

		leftHandExpression.applyInferableType( expressibleType );
		rightHandExpression.applyInferableType( expressibleType );
	}

	private SqmComparisonPredicate(SqmComparisonPredicate affirmativeForm) {
		super( true, affirmativeForm.nodeBuilder() );
		this.leftHandExpression = affirmativeForm.leftHandExpression;
		this.rightHandExpression = affirmativeForm.rightHandExpression;
		this.operator = affirmativeForm.operator;
		assertComparable( leftHandExpression, rightHandExpression, nodeBuilder() );
	}

	@Override
	public SqmComparisonPredicate copy(SqmCopyContext context) {
		final SqmComparisonPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmComparisonPredicate predicate = context.registerCopy(
				this,
				new SqmComparisonPredicate(
						leftHandExpression.copy( context ),
						operator,
						rightHandExpression.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
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
	public void negate() {
		this.operator = this.operator.negated();
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmComparisonPredicate( this );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitComparisonPredicate( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		leftHandExpression.appendHqlString( hql );
		hql.append( ' ' );
		hql.append( operator.sqlText() );
		hql.append( ' ' );
		rightHandExpression.appendHqlString( hql );
	}
}

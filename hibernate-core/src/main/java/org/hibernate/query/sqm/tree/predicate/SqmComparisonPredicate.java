/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

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
	public void appendHqlString(StringBuilder sb) {
		leftHandExpression.appendHqlString( sb );
		sb.append( ' ' );
		sb.append( operator.sqlText() );
		sb.append( ' ' );
		rightHandExpression.appendHqlString( sb );
	}
}

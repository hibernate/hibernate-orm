/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import static java.util.Objects.requireNonNull;
import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

/**
 * @author Steve Ebersole
 */
public class SqmComparisonPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> leftHandExpression;
	private ComparisonOperator operator;
	private final SqmExpression<?> rightHandExpression;

	public SqmComparisonPredicate(
			@Nonnull SqmExpression<?> leftHandExpression,
			@Nonnull ComparisonOperator operator,
			@Nonnull SqmExpression<?> rightHandExpression,
			@Nonnull NodeBuilder nodeBuilder) {
		this( leftHandExpression, operator, rightHandExpression, false, nodeBuilder );
	}

	private SqmComparisonPredicate(
			@Nonnull SqmExpression<?> leftHandExpression,
			@Nonnull ComparisonOperator operator,
			@Nonnull SqmExpression<?> rightHandExpression,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );

		// CriteriaBuilder does not check its arguments, so check these here instead
		requireNonNull( operator, "Operator must not be null" );
		requireNonNull( leftHandExpression, "Left expression must not be null" );
		requireNonNull( rightHandExpression, "Right expression must not be null" );

		this.leftHandExpression = leftHandExpression;
		this.rightHandExpression = rightHandExpression;
		this.operator = operator;

		assertComparable( leftHandExpression, rightHandExpression, nodeBuilder );

		final SqmBindableType<?> expressibleType = QueryHelper.highestPrecedenceType(
				leftHandExpression.getExpressible(),
				rightHandExpression.getExpressible()
		);

		leftHandExpression.applyInferableType( expressibleType );
		rightHandExpression.applyInferableType( expressibleType );
	}

	private SqmComparisonPredicate(@Nonnull SqmComparisonPredicate affirmativeForm) {
		super( true, affirmativeForm.nodeBuilder() );
		this.leftHandExpression = affirmativeForm.leftHandExpression;
		this.rightHandExpression = affirmativeForm.rightHandExpression;
		this.operator = affirmativeForm.operator;
		assertComparable( leftHandExpression, rightHandExpression, affirmativeForm.nodeBuilder() );
	}

	@Nonnull
	@Override
	public SqmComparisonPredicate copy(@Nonnull SqmCopyContext context) {
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

	@Nonnull
	public SqmExpression<?> getLeftHandExpression() {
		return leftHandExpression;
	}

	@Nonnull
	public SqmExpression<?> getRightHandExpression() {
		return rightHandExpression;
	}

	@Nonnull
	public ComparisonOperator getSqmOperator() {
		return operator;
	}

	@Override
	public void negate() {
		this.operator = this.operator.negated();
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmComparisonPredicate( this );
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitComparisonPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		leftHandExpression.appendHqlString( hql, context );
		hql.append( ' ' );
		hql.append( operator.sqlText() );
		hql.append( ' ' );
		rightHandExpression.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmComparisonPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.operator == that.operator
			&& this.leftHandExpression.equals( that.leftHandExpression )
			&& this.rightHandExpression.equals( that.rightHandExpression );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + operator.hashCode();
		result = 31 * result + leftHandExpression.hashCode();
		result = 31 * result + rightHandExpression.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmComparisonPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.operator == that.operator
			&& this.leftHandExpression.isCompatible( that.leftHandExpression )
			&& this.rightHandExpression.isCompatible( that.rightHandExpression );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + operator.hashCode();
		result = 31 * result + leftHandExpression.cacheHashCode();
		result = 31 * result + rightHandExpression.cacheHashCode();
		return result;
	}
}

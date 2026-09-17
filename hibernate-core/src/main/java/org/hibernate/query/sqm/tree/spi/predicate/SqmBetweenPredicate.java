/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;


import static org.hibernate.query.sqm.internal.TypecheckUtil.assertComparable;

/**
 * @author Steve Ebersole
 */
public class SqmBetweenPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;
	private final SqmExpression<?> lowerBound;
	private final SqmExpression<?> upperBound;

	public SqmBetweenPredicate(
			@Nonnull SqmExpression<?> expression,
			@Nonnull SqmExpression<?> lowerBound,
			@Nonnull SqmExpression<?> upperBound,
			boolean negated,
			@Nonnull NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;

		assertComparable( expression, lowerBound, nodeBuilder );
		assertComparable( expression, upperBound, nodeBuilder );

		final SqmBindableType<?> expressibleType = QueryHelper.highestPrecedenceType(
				expression.getExpressible(),
				lowerBound.getExpressible(),
				upperBound.getExpressible()
		);

		expression.applyInferableType( expressibleType );
		lowerBound.applyInferableType( expressibleType );
		upperBound.applyInferableType( expressibleType );
	}

	@Nonnull
	@Override
	public SqmBetweenPredicate copy(@Nonnull SqmCopyContext context) {
		final SqmBetweenPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmBetweenPredicate predicate = context.registerCopy(
				this,
				new SqmBetweenPredicate(
						expression.copy( context ),
						lowerBound.copy( context ),
						upperBound.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Nonnull
	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Nonnull
	public SqmExpression<?> getLowerBound() {
		return lowerBound;
	}

	@Nonnull
	public SqmExpression<?> getUpperBound() {
		return upperBound;
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitBetweenPredicate( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		if ( isNegated() ) {
			hql.append( " not" );
		}
		hql.append( " between " );
		lowerBound.appendHqlString( hql, context );
		hql.append( " and " );
		upperBound.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmBetweenPredicate that
			&& this.isNegated() == that.isNegated()
			&& expression.equals( that.expression )
			&& lowerBound.equals( that.lowerBound )
			&& upperBound.equals( that.upperBound );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + expression.hashCode();
		result = 31 * result + lowerBound.hashCode();
		result = 31 * result + upperBound.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmBetweenPredicate that
			&& this.isNegated() == that.isNegated()
			&& expression.isCompatible( that.expression )
			&& lowerBound.isCompatible( that.lowerBound )
			&& upperBound.isCompatible( that.upperBound );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + expression.cacheHashCode();
		result = 31 * result + lowerBound.cacheHashCode();
		result = 31 * result + upperBound.cacheHashCode();
		return result;
	}

	@Nonnull
	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmBetweenPredicate( expression, lowerBound, upperBound, ! isNegated(), nodeBuilder() );
	}
}

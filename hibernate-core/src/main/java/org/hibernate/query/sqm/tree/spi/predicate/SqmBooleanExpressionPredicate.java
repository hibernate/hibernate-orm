/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.predicate;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;

/**
 * Represents an expression whose type is boolean, and can therefore be used as a predicate.
 * E.g. {@code `from Employee e where e.isActive`}
 *
 * @author Steve Ebersole
 */
public class SqmBooleanExpressionPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<Boolean> booleanExpression;

	public SqmBooleanExpressionPredicate(
			SqmExpression<Boolean> booleanExpression,
			NodeBuilder nodeBuilder) {
		this( booleanExpression, false, nodeBuilder );
	}

	public SqmBooleanExpressionPredicate(
			SqmExpression<Boolean> booleanExpression,
			boolean negated,
			NodeBuilder nodeBuilder) {
		super( booleanExpression.getExpressible(), negated, nodeBuilder );

		assert isBooleanExpression( booleanExpression );
		this.booleanExpression = booleanExpression;
	}

	private static boolean isBooleanExpression(SqmExpression<Boolean> expression) {
		final SqmBindableType<Boolean> nodeType = expression.getNodeType();
		final Class<?> expressionJavaType =
				nodeType != null ? nodeType.getExpressibleJavaType().getJavaTypeClass() : Boolean.class;
		return boolean.class.equals( expressionJavaType ) || Boolean.class.equals( expressionJavaType );
	}

	@Override
	public SqmBooleanExpressionPredicate copy(SqmCopyContext context) {
		final SqmBooleanExpressionPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmBooleanExpressionPredicate predicate = context.registerCopy(
				this,
				new SqmBooleanExpressionPredicate(
						booleanExpression.copy( context ),
						isNegated(),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	public SqmExpression<Boolean> getBooleanExpression() {
		return booleanExpression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBooleanExpressionPredicate( this );
	}

	@Nonnull
	@Override
	public List<Expression<Boolean>> getExpressions() {
		final List<Expression<Boolean>> expressions = new ArrayList<>( 1 );
		expressions.add( booleanExpression );
		return expressions;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		booleanExpression.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmBooleanExpressionPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.booleanExpression.equals( that.booleanExpression );
	}

	@Override
	public int hashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + booleanExpression.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmBooleanExpressionPredicate that
			&& this.isNegated() == that.isNegated()
			&& this.booleanExpression.isCompatible( that.booleanExpression );
	}

	@Override
	public int cacheHashCode() {
		int result = Boolean.hashCode( isNegated() );
		result = 31 * result + booleanExpression.cacheHashCode();
		return result;
	}

	@Override
	protected SqmNegatablePredicate createNegatedNode() {
		return new SqmBooleanExpressionPredicate( booleanExpression, !isNegated(), nodeBuilder() );
	}

	@Override
	public String toString() {
		return isNegated()
				? "SqmBooleanExpressionPredicate( (not) " + booleanExpression + " )"
				: "SqmBooleanExpressionPredicate( " + booleanExpression + " )";
	}
}

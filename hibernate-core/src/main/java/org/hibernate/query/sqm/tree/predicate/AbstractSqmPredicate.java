/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaNumericExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;

import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpression;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpressionImplementor;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPredicate
		extends AbstractSqmExpression<Boolean>
		implements SqmPredicate, SqmBooleanExpressionImplementor {

	public AbstractSqmPredicate(@Nullable SqmBindableType<Boolean> type, NodeBuilder nodeBuilder) {
		super( type == null ? nodeBuilder.getBooleanType() : type, nodeBuilder );
	}

	@Override
	public @Nonnull JavaType<Boolean> getJavaTypeDescriptor(){
		return castNonNull( super.getJavaTypeDescriptor() );
	}

	@Override
	public @Nonnull JavaType<Boolean> getNodeJavaType() {
		return castNonNull( super.getNodeJavaType() );
	}

	@Override
	public @Nonnull SqmBindableType<Boolean> getExpressible() {
		return castNonNull( super.getExpressible() );
	}

	@Override
	public @Nonnull SqmBindableType<Boolean> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Nonnull
	@Override
	public BooleanOperator getOperator() {
		// most predicates are conjunctive
		return BooleanOperator.AND;
	}

	@Nonnull
	@Override
	public List<Expression<Boolean>> getExpressions() {
		/// most predicates do not have sub-predicates
		return new ArrayList<>(0);
	}

	@Nonnull
	@Override
	public JpaNumericExpression<Long> count() {
		throw new UnsupportedOperationException( "Cannot apply `count()` to predicates" );
	}

	@Nonnull
	@Override
	public JpaNumericExpression<Long> countDistinct() {
		throw new UnsupportedOperationException( "Cannot apply `countDistinct()` to predicates" );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y) {
		return (SqmBooleanExpression) super.coalesce( y );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression coalesce(Boolean y) {
		return (SqmBooleanExpression) super.coalesce( y );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y) {
		return (SqmBooleanExpression) super.nullif( y );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression nullif(Boolean y) {
		return (SqmBooleanExpression) super.nullif( y );
	}

	@Nonnull
	@Override
	public SqmPredicate and(@Nonnull Expression<Boolean> y) {
		return nodeBuilder().and( this, y );
	}

	@Nonnull
	@Override
	public SqmPredicate or(@Nonnull Expression<Boolean> y) {
		return nodeBuilder().or( this, y );
	}

	@Nonnull
	@Override
	public SqmPredicate not() {
		return nodeBuilder().not( this );
	}


}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
	public @NonNull JavaType<Boolean> getJavaTypeDescriptor(){
		return castNonNull( super.getJavaTypeDescriptor() );
	}

	@Override
	public @NonNull JavaType<Boolean> getNodeJavaType() {
		return castNonNull( super.getNodeJavaType() );
	}

	@Override
	public @NonNull SqmBindableType<Boolean> getExpressible() {
		return castNonNull( super.getExpressible() );
	}

	@Override
	public @NonNull SqmBindableType<Boolean> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Override
	public BooleanOperator getOperator() {
		// most predicates are conjunctive
		return BooleanOperator.AND;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		/// most predicates do not have sub-predicates
		return new ArrayList<>(0);
	}

	@Override
	public JpaNumericExpression<Long> count() {
		throw new UnsupportedOperationException( "Cannot apply `count()` to predicates" );
	}

	@Override
	public JpaNumericExpression<Long> countDistinct() {
		throw new UnsupportedOperationException( "Cannot apply `countDistinct()` to predicates" );
	}

	@Override
	public SqmBooleanExpression coalesce(Expression<? extends Boolean> y) {
		return (SqmBooleanExpression) super.coalesce( y );
	}

	@Override
	public SqmBooleanExpression coalesce(Boolean y) {
		return (SqmBooleanExpression) super.coalesce( y );
	}

	@Override
	public SqmBooleanExpression nullif(Expression<? extends Boolean> y) {
		return (SqmBooleanExpression) super.nullif( y );
	}

	@Override
	public SqmBooleanExpression nullif(Boolean y) {
		return (SqmBooleanExpression) super.nullif( y );
	}

	@Override
	public SqmPredicate and(Expression<Boolean> y) {
		return nodeBuilder().and( this, y );
	}

	@Override
	public SqmPredicate or(Expression<Boolean> y) {
		return nodeBuilder().or( this, y );
	}

	@Override
	public SqmPredicate not() {
		return nodeBuilder().not( this );
	}


}

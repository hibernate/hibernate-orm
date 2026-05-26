/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpression;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpressionImplementor;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpressionWrapper;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmBooleanValuedSimplePath
		extends SqmComparableValuedSimplePath<Boolean>
		implements SqmBooleanPath, SqmBooleanExpressionImplementor {
	public SqmBooleanValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<Boolean> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmBooleanValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<Boolean> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	protected SqmBooleanValuedSimplePath createCopy(
			NavigablePath navigablePath,
			SqmPathSource<Boolean> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		return new SqmBooleanValuedSimplePath(
				navigablePath,
				referencedPathSource,
				lhs,
				explicitAlias,
				nodeBuilder
		);
	}

	@Nonnull
	@Override
	public SqmBooleanExpression coalesce(@Nonnull Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression coalesce(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression nullif(@Nonnull Expression<? extends Boolean> y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression nullif(Boolean y) {
		return new SqmBooleanExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression max() {
		throw new UnsupportedOperationException( "Boolean expression does not support max()" );
	}

	@Nonnull
	@Override
	public SqmBooleanExpression min() {
		throw new UnsupportedOperationException( "Boolean expression does not support min()" );
	}
}

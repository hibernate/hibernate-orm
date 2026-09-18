/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpressionImplementor;
import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpressionWrapper;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmNumericValuedSimplePath<N extends Number & Comparable<N>>
		extends SqmComparableValuedSimplePath<N>
		implements SqmNumericPath<N>, SqmNumericExpressionImplementor<N> {
	public SqmNumericValuedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<N> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nonnull NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmNumericValuedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<N> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nullable String explicitAlias,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Nonnull
	@Override
	protected SqmNumericValuedSimplePath<N> createCopy(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<N> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nullable String explicitAlias,
			@Nonnull NodeBuilder nodeBuilder) {
		return new SqmNumericValuedSimplePath<>(
				navigablePath,
				referencedPathSource,
				lhs,
				explicitAlias,
				nodeBuilder
		);
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> coalesce(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> coalesce(@Nonnull N y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> nullif(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> nullif(@Nonnull N y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

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
			NavigablePath navigablePath,
			SqmPathSource<N> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmNumericValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<N> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	protected SqmNumericValuedSimplePath<N> createCopy(
			NavigablePath navigablePath,
			SqmPathSource<N> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
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
	public SqmNumericExpression<N> coalesce(N y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> nullif(@Nonnull Expression<? extends N> y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmNumericExpression<N> nullif(N y) {
		return new SqmNumericExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

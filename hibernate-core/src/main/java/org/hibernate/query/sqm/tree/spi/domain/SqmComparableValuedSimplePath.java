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
import org.hibernate.query.sqm.tree.spi.expression.SqmComparableExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmComparableExpressionImplementor;
import org.hibernate.query.sqm.tree.spi.expression.SqmComparableExpressionWrapper;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmComparableValuedSimplePath<C extends Comparable<? super C>>
		extends SqmBasicValuedSimplePath<C>
		implements SqmComparableExpressionImplementor<C> {
	public SqmComparableValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<C> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmComparableValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<C> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	protected SqmComparableValuedSimplePath<C> createCopy(
			NavigablePath navigablePath,
			SqmPathSource<C> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		return new SqmComparableValuedSimplePath<>(
				navigablePath,
				referencedPathSource,
				lhs,
				explicitAlias,
				nodeBuilder
		);
	}

	@Nonnull
	@Override
	public SqmComparableExpression<C> coalesce(@Nonnull Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmComparableExpression<C> coalesce(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmComparableExpression<C> nullif(@Nonnull Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmComparableExpression<C> nullif(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

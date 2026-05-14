/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.expression.SqmComparableExpression;
import org.hibernate.query.sqm.tree.expression.SqmComparableExpressionImplementor;
import org.hibernate.query.sqm.tree.expression.SqmComparableExpressionWrapper;
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

	@Override
	public SqmComparableExpression<C> coalesce(Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmComparableExpression<C> coalesce(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmComparableExpression<C> nullif(Expression<? extends C> y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmComparableExpression<C> nullif(C y) {
		return new SqmComparableExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}

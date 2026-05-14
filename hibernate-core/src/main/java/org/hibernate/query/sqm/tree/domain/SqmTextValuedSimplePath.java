/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.expression.SqmTextExpression;
import org.hibernate.query.sqm.tree.expression.SqmTextExpressionImplementor;
import org.hibernate.query.sqm.tree.expression.SqmTextExpressionWrapper;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTextValuedSimplePath
		extends SqmComparableValuedSimplePath<String>
		implements SqmTextPath, SqmTextExpressionImplementor {
	public SqmTextValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<String> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmTextValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<String> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	protected SqmTextValuedSimplePath createCopy(
			NavigablePath navigablePath,
			SqmPathSource<String> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		return new SqmTextValuedSimplePath(
				navigablePath,
				referencedPathSource,
				lhs,
				explicitAlias,
				nodeBuilder
		);
	}

	@Override
	public SqmTextExpression coalesce(Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmTextExpression coalesce(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().coalesce( this, y ) );
	}

	@Override
	public SqmTextExpression nullif(Expression<? extends String> y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}

	@Override
	public SqmTextExpression nullif(String y) {
		return new SqmTextExpressionWrapper( nodeBuilder().nullif( this, y ) );
	}
}

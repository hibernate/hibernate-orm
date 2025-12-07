/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSimplePath<T> extends AbstractSqmPath<T> implements SqmSimplePath<T> {

	public AbstractSqmSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	// The call to setExplicitAlias() is safe, so ignore the uninitialized error
	@SuppressWarnings({"uninitialized", "method.invocation"})
	public AbstractSqmSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
		setExplicitAlias( explicitAlias );
	}

	@Override
	public @NonNull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	protected @NonNull NavigablePath getParentNavigablePath() {
		// Since the LHS is non-null, we know that the navigable path must have a parent
		return castNonNull( getNavigablePath().getParent() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		getLhs().appendHqlString( hql, context );
		hql.append( '.' );
		hql.append( getReferencedPathSource().getPathName() );
	}

	@Override
	public @NonNull SqmBindableType<T> getNodeType() {
		return getReferencedPathSource().getExpressible();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		final var pathSource = super.getReferencedPathSource();
		return pathSource instanceof SqmSingularPersistentAttribute<?, T> attribute
				? attribute.getSqmPathSource()
				: pathSource;
	}

	@Override
	public SqmPathSource<T> getModel() {
		return super.getReferencedPathSource();
	}
}

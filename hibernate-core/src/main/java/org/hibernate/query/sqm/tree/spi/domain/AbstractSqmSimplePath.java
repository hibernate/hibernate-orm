/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmPathSource;

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
	public @Nonnull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	protected @Nonnull NavigablePath getParentNavigablePath() {
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
	public @Nonnull SqmBindableType<T> getNodeType() {
		return getReferencedPathSource().getExpressible();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		final var pathSource = super.getReferencedPathSource();
		return pathSource instanceof SqmSingularPersistentAttribute<?, T> attribute
				? attribute.getSqmPathSource()
				: pathSource;
	}

	@Nonnull
	@Override
	public SqmPathSource<T> getModel() {
		return super.getReferencedPathSource();
	}
}

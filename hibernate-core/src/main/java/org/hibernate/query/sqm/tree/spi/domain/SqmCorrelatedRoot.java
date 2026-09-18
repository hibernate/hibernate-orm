/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedRoot<T> extends SqmRoot<T> implements SqmPathWrapper<T, T>, SqmCorrelation<T, T> {

	private final SqmRoot<T> correlationParent;

	public SqmCorrelatedRoot(@Nonnull SqmRoot<T> correlationParent) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getModel(),
				correlationParent.getExplicitAlias(),
				correlationParent.nodeBuilder()
		);
		this.correlationParent = correlationParent;
	}

	protected SqmCorrelatedRoot(@Nonnull NavigablePath navigablePath, @Nonnull SqmPathSource<T> referencedNavigable, @Nonnull NodeBuilder nodeBuilder, @Nonnull SqmRoot<T> correlationParent) {
		super( navigablePath, referencedNavigable, nodeBuilder );
		this.correlationParent = correlationParent;
	}

	@Nonnull
	@Override
	public SqmCorrelatedRoot<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCorrelatedRoot<>( correlationParent.copy( context ) )
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmRoot<T> getCorrelationParent() {
		return correlationParent;
	}

	@Nonnull
	@Override
	public SqmPath<T> getWrappedPath() {
		return getCorrelationParent();
	}

	@Override
	public @Nullable String getExplicitAlias() {
		return correlationParent.getExplicitAlias();
	}

	@Override
	public void setExplicitAlias(@Nullable String explicitAlias) {
		throw new UnsupportedOperationException( "Can't set alias on a correlated root" );
	}

	@Nonnull
	@Override
	public JpaSelection<T> alias(@Nonnull String name) {
		setAlias( name );
		return this;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Nonnull
	@Override
	public SqmRoot<T> getCorrelatedRoot() {
		return this;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedRoot( this );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedRoot<?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedRoot<?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}
}

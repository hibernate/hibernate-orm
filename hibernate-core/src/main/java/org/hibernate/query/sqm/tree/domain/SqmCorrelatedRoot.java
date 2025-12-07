/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCorrelatedRoot<T> extends SqmRoot<T> implements SqmPathWrapper<T, T>, SqmCorrelation<T, T> {

	private final SqmRoot<T> correlationParent;

	public SqmCorrelatedRoot(SqmRoot<T> correlationParent) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getModel(),
				correlationParent.getExplicitAlias(),
				correlationParent.nodeBuilder()
		);
		this.correlationParent = correlationParent;
	}

	protected SqmCorrelatedRoot(NavigablePath navigablePath, SqmPathSource<T> referencedNavigable, NodeBuilder nodeBuilder, SqmRoot<T> correlationParent) {
		super( navigablePath, referencedNavigable, nodeBuilder );
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmCorrelatedRoot<T> copy(SqmCopyContext context) {
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

	@Override
	public SqmRoot<T> getCorrelationParent() {
		return correlationParent;
	}

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

	@Override
	public JpaSelection<T> alias(String name) {
		setAlias( name );
		return this;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public SqmRoot<T> getCorrelatedRoot() {
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCorrelatedRoot( this );
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& other instanceof SqmCorrelatedRoot<?> that
			&& correlationParent.equals( that.correlationParent );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& other instanceof SqmCorrelatedRoot<?> that
			&& correlationParent.isCompatible( that.correlationParent );
	}
}

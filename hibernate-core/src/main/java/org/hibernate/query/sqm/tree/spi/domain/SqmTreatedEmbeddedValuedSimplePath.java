/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEmbeddedValuedSimplePath<T, S extends T> extends SqmEmbeddedValuedSimplePath<S>
		implements SqmTreatedPath<T, S> {
	private final SqmPath<T> wrappedPath;
	private final SqmEmbeddableDomainType<S> treatTarget;

	@SuppressWarnings("unchecked")
	public SqmTreatedEmbeddedValuedSimplePath(
			@Nonnull SqmPath<T> wrappedPath,
			@Nonnull SqmEmbeddableDomainType<S> treatTarget) {
		super(
				wrappedPath.getNavigablePath().treatAs( treatTarget.getTypeName() ),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@SuppressWarnings("unchecked")
	private SqmTreatedEmbeddedValuedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPath<T> wrappedPath,
			@Nonnull SqmEmbeddableDomainType<S> treatTarget) {
		super(
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Nonnull
	@Override
	public SqmTreatedEmbeddedValuedSimplePath<T, S> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedEmbeddedValuedSimplePath<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmTreatableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @Nonnull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmTreatableDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		// Cast needed for static nullness analysis.
		return walker.visitTreatedPath( (SqmTreatedPath<?, ?>) this );
	}

	@Nonnull
	@Override
	public SqmPath<?> resolvePathPart(@Nonnull String name, boolean isTerminal, @Nonnull SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getTypeName() );
		hql.append( ')' );
	}
}

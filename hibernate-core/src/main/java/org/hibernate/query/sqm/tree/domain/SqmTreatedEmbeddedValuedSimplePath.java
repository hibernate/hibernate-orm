/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
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
			SqmPath<T> wrappedPath,
			SqmEmbeddableDomainType<S> treatTarget) {
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
			NavigablePath navigablePath,
			SqmPath<T> wrappedPath,
			SqmEmbeddableDomainType<S> treatTarget) {
		super(
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedEmbeddedValuedSimplePath<T, S> copy(SqmCopyContext context) {
		final SqmTreatedEmbeddedValuedSimplePath<T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedEmbeddedValuedSimplePath<T, S> path = context.registerCopy(
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

	@Override
	public SqmTreatableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @NonNull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public SqmTreatableDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		// Cast needed for Checker Framework
		return walker.visitTreatedPath( (SqmTreatedPath<?, @Nullable ?>) this );
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getTypeName() );
		hql.append( ')' );
	}
}

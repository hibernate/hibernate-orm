/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedRoot<E, S extends E>
		extends SqmRoot<S>
		implements SqmTreatedFrom<S, E, S> {
	private final SqmRoot<E> wrappedPath;
	private final SqmEntityDomainType<S> treatTarget;

	public SqmTreatedRoot(
			SqmRoot<E> wrappedPath,
			SqmEntityDomainType<S> treatTarget) {
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				treatTarget,
				null,
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	private SqmTreatedRoot(
			NavigablePath navigablePath,
			SqmRoot<E> wrappedPath,
			SqmEntityDomainType<S> treatTarget) {
		super(
				navigablePath,
				treatTarget,
				null,
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	@Nonnull
	public SqmTreatedRoot<E, S> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedRoot<>(
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
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getManagedType() {
		return getTreatTarget();
	}

	@Override
	public SqmRoot<E> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @Nonnull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmEntityDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Nullable
	@Override
	public SqmPath<?> getLhs() {
		return wrappedPath.getLhs();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTreatedPath( this );
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedFrom<S, S, S1> treatAs(
			@Nonnull EntityDomainType<S1> treatTarget,
			@Nullable String alias,
			boolean fetch) {
		@SuppressWarnings("unchecked")
		final var treat = (SqmTreatedFrom<S, S, S1>)
				wrappedPath.treatAs( treatTarget, alias, fetch );
		return treat;
	}
}

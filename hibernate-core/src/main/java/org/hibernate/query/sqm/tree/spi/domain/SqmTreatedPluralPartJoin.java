/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedPluralPartJoin<O, T, S extends T>
		extends SqmPluralPartJoin<O, S>
		implements SqmTreatedJoin<O, T, S> {
	private final SqmPluralPartJoin<O, T> wrappedPath;
	private final SqmEntityDomainType<S> treatTarget;

	public SqmTreatedPluralPartJoin(
			SqmPluralPartJoin<O, T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			@Nullable String alias) {
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getHibernateEntityName(), alias ),
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedPluralPartJoin(
			NavigablePath navigablePath,
			SqmPluralPartJoin<O, T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			@Nullable String alias) {
		super(
				wrappedPath.getLhs(),
				navigablePath,
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	@Nonnull
	public SqmTreatedPluralPartJoin<O, T, S> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedPluralPartJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmPluralPartJoin<O, T> getWrappedPath() {
		return wrappedPath;
	}

	@Nonnull
	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public @Nonnull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedPluralPartJoin<O, S, S1> treatAs(
			@Nonnull Class<S1> treatJavaType,
			@Nullable String alias,
			boolean fetch) {
		@SuppressWarnings("unchecked")
		final var treat = (SqmTreatedPluralPartJoin<O, S, S1>)
				wrappedPath.treatAs( treatJavaType, alias, fetch );
		return treat;
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedPluralPartJoin<O, S, S1> treatAs(
			@Nonnull EntityDomainType<S1> treatTarget,
			@Nullable String alias,
			boolean fetch) {
		@SuppressWarnings("unchecked")
		final var treat = (SqmTreatedPluralPartJoin<O, S, S1>)
				wrappedPath.treatAs( treatTarget, alias, fetch );
		return treat;
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}
}

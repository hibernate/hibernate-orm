/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class SqmTreatedPluralPartJoin extends SqmPluralPartJoin implements SqmTreatedJoin {
	private final SqmPluralPartJoin<?,?> wrappedPath;
	private final SqmEntityDomainType<?> treatTarget;

	public SqmTreatedPluralPartJoin(
			SqmPluralPartJoin<?,?> wrappedPath,
			SqmEntityDomainType<?> treatTarget,
			@Nullable String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getHibernateEntityName(), alias ),
				wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedPluralPartJoin(
			NavigablePath navigablePath,
			SqmPluralPartJoin<?,?> wrappedPath,
			SqmEntityDomainType<?> treatTarget,
			@Nullable String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	@Nonnull
	public SqmTreatedPluralPartJoin copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedPluralPartJoin(
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
	public SqmPluralPartJoin<?,?> getWrappedPath() {
		return wrappedPath;
	}

	@Nonnull
	@Override
	public EntityDomainType<?> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public @Nonnull SqmBindableType<?> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	@Nonnull
	public SqmTreatedPluralPartJoin treatAs(@Nonnull Class treatJavaType, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return wrappedPath.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	@Nonnull
	public SqmTreatedPluralPartJoin treatAs(@Nonnull EntityDomainType treatTarget, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return wrappedPath.treatAs( treatTarget, alias, fetch );
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

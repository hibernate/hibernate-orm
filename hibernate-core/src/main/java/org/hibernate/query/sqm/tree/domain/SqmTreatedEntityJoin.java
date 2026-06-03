/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityJoin<L,R,S extends R> extends SqmEntityJoin<L,S> implements SqmTreatedJoin<L,R,S> {
	private final SqmEntityJoin<L,R> wrappedPath;
	private final SqmEntityDomainType<S> treatTarget;

	public SqmTreatedEntityJoin(
			SqmEntityJoin<L,R> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			@Nullable String alias) {
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName(),
						alias
				),
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	private SqmTreatedEntityJoin(
			NavigablePath navigablePath,
			SqmEntityJoin<L,R> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			@Nullable String alias) {
		super(
				navigablePath,
				treatTarget,
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	@Nonnull
	public SqmTreatedEntityJoin<L,R,S> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedEntityJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias()
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

	@Nonnull
	@Override
	public SqmEntityDomainType<S> getModel() {
		return treatTarget;
	}

	@Override
	public SqmPath<R> getWrappedPath() {
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
	public SqmTreatedEntityJoin<L,R,S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restriction );
	}

	@Nonnull
	@Override
	public SqmTreatedEntityJoin<L,R,S> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmTreatedEntityJoin<L,R,S> on(@Nullable JpaPredicate ... restrictions) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmTreatedEntityJoin<L,R,S> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmTreatedEntityJoin<L,R,S>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedEntityJoin<L, S, S1> treatAs(@Nonnull Class<S1> treatJavaType, @Nullable String alias, boolean fetched) {
		//noinspection unchecked
		return (SqmTreatedEntityJoin<L, S, S1>) wrappedPath.treatAs( treatJavaType, alias, fetched );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedEntityJoin<L, S, S1> treatAs(@Nonnull EntityDomainType<S1> treatTarget, @Nullable String alias, boolean fetched) {
		//noinspection unchecked
		return (SqmTreatedEntityJoin<L, S, S1>) wrappedPath.treatAs( treatTarget, alias, fetched );
	}
}

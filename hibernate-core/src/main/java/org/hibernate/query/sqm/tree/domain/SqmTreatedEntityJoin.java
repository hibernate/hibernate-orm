/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmEntityDomainType<S> getModel() {
		return treatTarget;
	}

	@Override
	public SqmPath<R> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @NonNull SqmBindableType<S> getNodeType() {
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
	public SqmTreatedEntityJoin<L,R,S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedEntityJoin<L,R,S> on(@Nullable Expression<Boolean> restriction) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedEntityJoin<L,R,S> on(JpaPredicate @Nullable ... restrictions) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedEntityJoin<L,R,S> on(Predicate @Nullable ... restrictions) {
		return (SqmTreatedEntityJoin<L, R, S>) super.on( restrictions );
	}

	@Override
	public <S1 extends S> SqmTreatedEntityJoin<L, S, S1> treatAs(Class<S1> treatJavaType, @Nullable String alias, boolean fetched) {
		//noinspection unchecked
		return (SqmTreatedEntityJoin<L, S, S1>) wrappedPath.treatAs( treatJavaType, alias, fetched );
	}

	@Override
	public <S1 extends S> SqmTreatedEntityJoin<L, S, S1> treatAs(EntityDomainType<S1> treatTarget, @Nullable String alias, boolean fetched) {
		//noinspection unchecked
		return (SqmTreatedEntityJoin<L, S, S1>) wrappedPath.treatAs( treatTarget, alias, fetched );
	}
}

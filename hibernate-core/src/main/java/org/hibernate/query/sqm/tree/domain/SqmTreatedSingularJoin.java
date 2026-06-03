/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSingularJoin<O,T, S extends T>
		extends SqmSingularJoin<O,S>
		implements SqmTreatedAttributeJoin<O,T,S> {
	private final SqmSingularJoin<O,T> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedSingularJoin(
			SqmSingularJoin<O,T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedSingularJoin(
			SqmSingularJoin<O,T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getTypeName(), alias ),
				(SqmSingularPersistentAttribute<O, S>)
						wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedSingularJoin(
			NavigablePath navigablePath,
			SqmSingularJoin<O,T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(SqmSingularPersistentAttribute<O, S>)
						wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	@Nonnull
	public SqmTreatedSingularJoin<O, T, S> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedSingularJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias(),
						isFetched()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSingularJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Nonnull
	@Override
	public TreatableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public @Nonnull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmSingularPersistentAttribute<? super O, S> getModel() {
		return (SqmSingularPersistentAttribute<? super O, S>) super.getReferencedPathSource();
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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getTypeName() );
		hql.append( ')' );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedSingularJoin<O, S, S1> treatAs(@Nonnull EntityDomainType<S1> treatTarget, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedSingularJoin<O, S, S1>) wrappedPath.treatAs( treatTarget, alias, fetch );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedSingularJoin<O, S, S1> treatAs(@Nonnull Class<S1> treatJavaType, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedSingularJoin<O, S, S1>) wrappedPath.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	@Nonnull
	public SqmTreatedSingularJoin<O,T,S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmTreatedSingularJoin<O,T,S> on(@Nullable JpaPredicate... restrictions) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmTreatedSingularJoin<O,T,S> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restriction );
	}

	@Nonnull
	@Override
	public SqmTreatedSingularJoin<O,T,S> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmTreatedSingularJoin<O,T,S>) super.on( restrictions );
	}
}

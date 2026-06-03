/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import org.hibernate.metamodel.mapping.CollectionPart;
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

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSetJoin<O,T, S extends T> extends SqmSetJoin<O,S> implements SqmTreatedAttributeJoin<O,T,S> {
	private final SqmSetJoin<O,T> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedSetJoin(
			SqmSetJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedSetJoin(
				SqmSetJoin<O, T> wrappedPath,
				SqmTreatableDomainType<S> treatTarget,
				@Nullable String alias,
				boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				(SqmSetPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedSetJoin(
			NavigablePath navigablePath,
			SqmSetJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(SqmSetPersistentAttribute<O, S>) wrappedPath.getAttribute(),
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
	public SqmTreatedSetJoin<O, T, S> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedSetJoin<>(
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
	public SqmSetJoin<O,T> getWrappedPath() {
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
	public SqmSetPersistentAttribute<O, S> getModel() {
		return (SqmSetPersistentAttribute<O, S>) super.getReferencedPathSource();
	}

	@Override
	public SqmTreatableDomainType<S> getReferencedPathSource() {
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
	public <S1 extends S> SqmTreatedSetJoin<O, S, S1> treatAs(@Nonnull Class<S1> treatJavaType, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedSetJoin<O, S, S1>) wrappedPath.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedSetJoin<O, S, S1> treatAs(@Nonnull EntityDomainType<S1> treatTarget, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedSetJoin<O, S, S1>) wrappedPath.treatAs( treatTarget, alias, fetch );
	}

	@Override
	@Nonnull
	public SqmTreatedSetJoin<O, T, S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restriction );
	}

	@Nonnull
	@Override
	public SqmTreatedSetJoin<O, T, S> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmTreatedSetJoin<O, T, S> on(@Nullable JpaPredicate... restrictions) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmTreatedSetJoin<O, T, S> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restrictions );
	}
}

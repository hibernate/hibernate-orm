/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedListJoin<O,T, S extends T> extends SqmListJoin<O,S> implements SqmTreatedAttributeJoin<O,T,S> {
	private final SqmListJoin<O,T> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedListJoin(
			SqmListJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedListJoin(
			SqmListJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				(SqmListPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedListJoin(
			NavigablePath navigablePath,
			SqmListJoin<O, T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(SqmListPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedListJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedListJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedListJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedListJoin<>(
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
	public SqmListJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public TreatableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public @NonNull SqmBindableType<S> getNodeType() {
		return treatTarget;
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
	public <S1 extends S> SqmTreatedListJoin<O, S, S1> treatAs(Class<S1> treatJavaType, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedListJoin<O, S, S1>) wrappedPath.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	public <S1 extends S> SqmTreatedListJoin<O, S, S1> treatAs(EntityDomainType<S1> treatTarget, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedListJoin<O, S, S1>) wrappedPath.treatAs( treatTarget, alias, fetch );
	}

	@Override
	public SqmTreatedListJoin<O,T,S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedListJoin<O,T,S>) super.on( restriction );
	}

	@Override
	public SqmTreatedListJoin<O,T,S> on(@Nullable Expression<Boolean> restriction) {
		return (SqmTreatedListJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedListJoin<O,T,S> on(JpaPredicate @Nullable... restrictions) {
		return (SqmTreatedListJoin<O, T, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedListJoin<O,T,S> on(Predicate @Nullable... restrictions) {
		return (SqmTreatedListJoin<O, T, S>) super.on( restrictions );
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

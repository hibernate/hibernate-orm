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
public class SqmTreatedMapJoin<L, K, V, S extends V> extends SqmMapJoin<L, K, S> implements SqmTreatedAttributeJoin<L,V,S> {
	private final SqmMapJoin<L, K, V> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedMapJoin(
			SqmMapJoin<L, K, V> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedMapJoin(
			SqmMapJoin<L, K, V> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				( (SqmMapJoin<L, K, S>) wrappedPath ).getModel(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedMapJoin(
			NavigablePath navigablePath,
			SqmMapJoin<L, K, V> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				( (SqmMapJoin<L, K, S>) wrappedPath ).getModel(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> copy(SqmCopyContext context) {
		final SqmTreatedMapJoin<L, K, V, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedMapJoin<L, K, V, S> path = context.registerCopy(
				this,
				new SqmTreatedMapJoin<>(
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
	public SqmMapJoin<L,K,V> getWrappedPath() {
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
	public <S1 extends S> SqmTreatedMapJoin<L, K, S, S1> treatAs(EntityDomainType<S1> treatTarget, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedMapJoin<L, K, S, S1>) wrappedPath.treatAs( treatTarget, alias, fetch );
	}

	@Override
	public <S1 extends S> SqmTreatedMapJoin<L, K, S, S1> treatAs(Class<S1> treatJavaType, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return (SqmTreatedMapJoin<L, K, S, S1>) wrappedPath.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(@Nullable Expression<Boolean> restriction) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(JpaPredicate @Nullable... restrictions) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(Predicate @Nullable... restrictions) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restrictions );
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Christian Beikov
 */
public class SqmPluralPartJoin<O,T> extends AbstractSqmJoin<O,T> {

	public SqmPluralPartJoin(
			SqmFrom<?,O> lhs,
			SqmPathSource<T> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super(
				SqmCreationHelper.buildSubNavigablePath( lhs, joinedNavigable.getPathName(), alias ),
				joinedNavigable,
				lhs,
				alias == SqmCreationHelper.IMPLICIT_ALIAS ? null : alias,
				joinType,
				nodeBuilder
		);
	}

	protected SqmPluralPartJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SqmPathSource<T> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super(
				navigablePath,
				joinedNavigable,
				lhs,
				alias == SqmCreationHelper.IMPLICIT_ALIAS ? null : alias,
				joinType,
				nodeBuilder
		);
	}

	@Override
	public boolean isImplicitlySelectable() {
		return false;
	}

	@Override
	public SqmPluralPartJoin<O, T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmPluralPartJoin<>(
						lhsCopy,
						getNavigablePathCopy( lhsCopy ),
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public @NonNull SqmFrom<?, O> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Override
	public @Nullable SqmPredicate getJoinPredicate() {
		return null;
	}

	@Override
	public void setJoinPredicate(@Nullable SqmPredicate predicate) {
		throw new UnsupportedOperationException( "Setting a predicate for a plural part join is unsupported" );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitPluralPartJoin( this );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		final SqmTreatedPluralPartJoin treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedPluralPartJoin( this, (SqmEntityDomainType<?>) treatTarget, alias ) );
		}
		return treat;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias, fetch );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		final SqmTreatedPluralPartJoin treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedPluralPartJoin( this, (SqmEntityDomainType<?>) treatTarget, alias ) );
		}
		return treat;
	}

	@Override
	public @Nullable PersistentAttribute<? super O, ?> getAttribute() {
		return null;
	}

	@Override
	public SqmCorrelatedPluralPartJoin<O, T> createCorrelation() {
		return new SqmCorrelatedPluralPartJoin<>( this );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SqmPluralPartJoin(%s : %s)",
				getNavigablePath(),
				getReferencedPathSource().getPathName()
		);
	}
}

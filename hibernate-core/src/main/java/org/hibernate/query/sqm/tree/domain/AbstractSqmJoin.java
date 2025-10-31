/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.JoinType;

import java.util.Objects;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.combinePredicates;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmJoin<L, R> extends AbstractSqmFrom<L, R> implements SqmJoin<L, R> {
	private final SqmJoinType joinType;
	private @Nullable SqmPredicate onClausePredicate;

	public AbstractSqmJoin(
			NavigablePath navigablePath,
			SqmPathSource<R> referencedNavigable,
			SqmFrom<?, L> lhs,
			@Nullable String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, alias, nodeBuilder );
		this.joinType = joinType;
	}

	@Override
	public SqmJoinType getSqmJoinType() {
		return joinType;
	}

	@Override
	public @Nullable SqmPredicate getJoinPredicate() {
		return onClausePredicate;
	}

	@Override
	public void setJoinPredicate(@Nullable SqmPredicate predicate) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Setting join predicate [%s] (was [%s])",
					predicate,
					this.onClausePredicate == null ? "<null>" : this.onClausePredicate
			);
		}

		this.onClausePredicate = predicate;
	}

	public void applyRestriction(SqmPredicate restriction) {
		if ( this.onClausePredicate == null ) {
			this.onClausePredicate = restriction;
		}
		else {
			this.onClausePredicate = combinePredicates( onClausePredicate, restriction );
		}
	}

	protected void copyTo(AbstractSqmJoin<L, R> target, SqmCopyContext context) {
		super.copyTo( target, context );
		target.onClausePredicate = onClausePredicate == null ? null : onClausePredicate.copy( context );
	}

	@Override
	public <S extends R> SqmTreatedJoin<L, R, S> treatAs(Class<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends R> SqmTreatedJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(Class<S> treatJavaType, @Nullable String alias);

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias);

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetched);

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched);

	@Override
	public @Nullable SqmFrom<?, L> getLhs() {
		//noinspection unchecked
		return (SqmFrom<?, L>) super.getLhs();
	}

	@Override
	public @Nullable SqmFrom<?, L> getParent() {
		return getLhs();
	}

	@Override
	public JoinType getJoinType() {
		return joinType.getCorrespondingJpaJoinType();
	}

	@Override
	public @Nullable SqmPredicate getOn() {
		return getJoinPredicate();
	}

	@Override
	public <X> SqmEntityJoin<R, X> join(Class<X> targetEntityClass) {
		return super.join( targetEntityClass, joinType );
	}

	@Override
	public <X> SqmEntityJoin<R, X> join(Class<X> targetEntityClass, SqmJoinType joinType) {
		return super.join( targetEntityClass, joinType );
	}

	// No need for equals/hashCode or isCompatible/cacheHashCode, because the base implementation using NavigablePath
	// is fine for the purpose of matching nodes "syntactically".

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& object instanceof AbstractSqmJoin<?,?> thatJoin
			&& joinType == thatJoin.getSqmJoinType()
			&& Objects.equals( onClausePredicate, thatJoin.getOn() );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& object instanceof AbstractSqmJoin<?,?> thatJoin
			&& joinType == thatJoin.getSqmJoinType()
			&& SqmCacheable.areCompatible( onClausePredicate, thatJoin.getOn() );
	}
}

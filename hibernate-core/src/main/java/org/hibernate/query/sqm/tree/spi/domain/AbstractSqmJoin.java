/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;

import java.util.List;
import java.util.Objects;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmJoin;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
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
					onClausePredicate == null ? "<null>" : this.onClausePredicate
			);
		}

		onClausePredicate = predicate;
	}

	public void applyRestriction(SqmPredicate restriction) {
		onClausePredicate =
				onClausePredicate == null
						? restriction
						: combinePredicates( onClausePredicate, restriction );
	}

	@Override
	@Nonnull
	public SqmJoin<L, R> on(@Nonnull BooleanExpression... restrictions) {
		setJoinPredicate( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	@Nonnull
	public SqmJoin<L, R> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		setJoinPredicate( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	@Nonnull
	public SqmJoin<L, R> on(@Nullable JpaExpression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmJoin<L, R> on(@Nonnull Expression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmJoin<L, R> on(@Nullable JpaPredicate ... restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	protected void copyTo(AbstractSqmJoin<L, R> target, SqmCopyContext context) {
		super.copyTo( target, context );
		target.onClausePredicate = onClausePredicate == null ? null : onClausePredicate.copy( context );
	}

	@Nonnull
	@Override
	public <S extends R> SqmTreatedJoin<L, R, S> treatAs(@Nonnull Class<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Nonnull
	@Override
	public <S extends R> SqmTreatedJoin<L, R, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	@Nonnull
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Override
	@Nonnull
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);

	@Override
	@Nonnull
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetched);

	@Override
	@Nonnull
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched);

	@Nullable
	@Override
	public SqmFrom<?, L> getLhs() {
		//noinspection unchecked
		return (SqmFrom<?, L>) super.getLhs();
	}

	@Override
	@Nonnull
	public SqmFrom<?, L> getParent() {
		return castNonNull( getLhs() );
	}

	@Override
	@Nonnull
	public JoinType getJoinType() {
		return joinType.getCorrespondingJpaJoinType();
	}

	@Nullable
	@Override
	public SqmPredicate getOn() {
		return getJoinPredicate();
	}

	@Override
	@Nonnull
	public <X> SqmEntityJoin<R, X> join(@Nonnull Class<X> targetEntityClass) {
		return join( targetEntityClass, joinType.getCorrespondingJpaJoinType() );
	}

	@Nonnull
	@Override
	public <X> SqmEntityJoin<R, X> join(@Nonnull Class<X> targetEntityClass, @Nonnull SqmJoinType joinType) {
		return join( targetEntityClass, joinType.getCorrespondingJpaJoinType() );
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.List;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;


/**
 * @author Steve Ebersole
 */
public interface SqmJoin<L, R> extends SqmFrom<L, R>, JpaJoin<L,R> {
	/**
	 * The type of join.
	 */
	SqmJoinType getSqmJoinType();

	/**
	 * When applicable, whether this join should be included in an implicit select clause
	 */
	boolean isImplicitlySelectable();

	/**
	 * Obtain the join predicate
	 *
	 * @return The join predicate
	 */
	@Nullable SqmPredicate getJoinPredicate();

	/**
	 * Inject the join predicate
	 *
	 * @param predicate The join predicate
	 */
	void setJoinPredicate(@Nullable SqmPredicate predicate);

	@Nonnull
	@Override
	<Y> SqmAttributeJoin<R, Y> join(@Nonnull String attributeName);

	@Nonnull
	@Override
	<Y> SqmAttributeJoin<R, Y> join(@Nonnull String attributeName, @Nonnull JoinType jt);

	@Override
	SqmJoin<L, R> copy(SqmCopyContext context);

	@Nonnull
	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(@Nonnull Class<S> treatAsType);

	@Nonnull
	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatAsType);

	@Override
	@Nonnull
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Override
	@Nonnull
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);

	@Nullable
	@Override
	default SqmPredicate getOn() {
		return getJoinPredicate();
	}

	@Override
	@Nonnull
	default SqmJoin<L, R> on(@Nullable JpaExpression<Boolean> restriction) {
		setJoinPredicate( restriction == null ? null : nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Nonnull
	@Override
	default SqmJoin<L, R> on(@Nonnull Expression<Boolean> restriction) {
		setJoinPredicate( restriction == null ? null : nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	@Nonnull
	default SqmJoin<L, R> on(@Nullable JpaPredicate... restrictions) {
		setJoinPredicate( restrictions == null ? null : nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Nonnull
	@Override
	SqmJoin<L, R> on(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	default SqmJoin<L, R> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		setJoinPredicate( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Nonnull
	default <X> JpaEntityJoin<R, X> join(@Nonnull Class<X> entityJavaType, @Nonnull SqmJoinType joinType) {
		return join( entityJavaType, joinType.getCorrespondingJpaJoinType() );
	}

	@Nonnull
	@Override
	<Y> JpaJoin<R, Y> join(@Nonnull EntityType<Y> entity);

	@Nonnull
	@Override
	<Y> JpaJoin<R, Y> join(@Nonnull EntityType<Y> entity, @Nonnull JoinType joinType);

	@Nonnull
	@Override
	<X> JpaEntityJoin<R, X> join(@Nonnull EntityDomainType<X> entity);

	@Nonnull
	@Override
	<X> JpaDerivedJoin<X> join(@Nonnull Subquery<X> subquery);

	@Nonnull
	@Override
	<X> JpaDerivedJoin<X> joinLateral(@Nonnull Subquery<X> subquery);

	@Nonnull
	@Override
	<X> JpaJoin<?, X> join(@Nonnull JpaCteCriteria<X> cte);

	@Nonnull
	@Override
	<X> JpaCrossJoin<R, X> crossJoin(@Nonnull Class<X> entityJavaType);

	@Nonnull
	@Override
	<X> JpaCrossJoin<R, X> crossJoin(@Nonnull EntityDomainType<X> entity);

	@Nonnull
	@Override
	<A> SqmSingularJoin<R, A> join(@Nonnull SingularAttribute<? super R, A> attribute);

	@Nonnull
	@Override
	<A> SqmSingularJoin<R, A> join(@Nonnull SingularAttribute<? super R, A> attribute, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<E> SqmBagJoin<R, E> join(@Nonnull CollectionAttribute<? super R, E> attribute);

	@Nonnull
	@Override
	<E> SqmBagJoin<R, E> join(@Nonnull CollectionAttribute<? super R, E> attribute, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<E> SqmSetJoin<R, E> join(@Nonnull SetAttribute<? super R, E> set);

	@Nonnull
	@Override
	<E> SqmSetJoin<R, E> join(@Nonnull SetAttribute<? super R, E> set, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<E> SqmListJoin<R, E> join(@Nonnull ListAttribute<? super R, E> list);

	@Nonnull
	@Override
	<E> SqmListJoin<R, E> join(@Nonnull ListAttribute<? super R, E> list, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<K, V> SqmMapJoin<R, K, V> join(@Nonnull MapAttribute<? super R, K, V> map);

	@Nonnull
	@Override
	<K, V> SqmMapJoin<R, K, V> join(@Nonnull MapAttribute<? super R, K, V> map, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<Y> SqmBagJoin<R, Y> joinCollection(@Nonnull String attributeName);

	@Nonnull
	@Override
	<Y> SqmBagJoin<R, Y> joinCollection(@Nonnull String attributeName, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<Y> SqmSetJoin<R, Y> joinSet(@Nonnull String attributeName);

	@Nonnull
	@Override
	<Y> SqmSetJoin<R, Y> joinSet(@Nonnull String attributeName, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<Y> SqmListJoin<R, Y> joinList(@Nonnull String attributeName);

	@Nonnull
	@Override
	<Y> SqmListJoin<R, Y> joinList(@Nonnull String attributeName, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<K, V> SqmMapJoin<R, K, V> joinMap(@Nonnull String attributeName);

	@Nonnull
	@Override
	<K, V> SqmMapJoin<R, K, V> joinMap(@Nonnull String attributeName, @Nonnull JoinType jt);
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.checkerframework.checker.nullness.qual.Nullable;
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
	 * The type of join - inner, cross, etc
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

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName, JoinType jt);

	@Override
	SqmJoin<L, R> copy(SqmCopyContext context);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(Class<S> treatAsType);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(EntityDomainType<S> treatAsType);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(Class<S> treatJavaType, @Nullable String alias);

	@Override
	<S extends R> SqmTreatedJoin<L,R,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias);

	@Override
	default @Nullable SqmPredicate getOn() {
		return getJoinPredicate();
	}

	@Override
	default SqmJoin<L, R> on(@Nullable JpaExpression<Boolean> restriction) {
		setJoinPredicate( restriction == null ? null : nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	default SqmJoin<L, R> on(@Nullable Expression<Boolean> restriction) {
		setJoinPredicate( restriction == null ? null : nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	default SqmJoin<L, R> on(JpaPredicate @Nullable... restrictions) {
		setJoinPredicate( restrictions == null ? null : nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	default SqmJoin<L, R> on(Predicate @Nullable... restrictions) {
		setJoinPredicate( restrictions == null ? null : nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	default <X> JpaEntityJoin<R, X> join(Class<X> entityJavaType, SqmJoinType joinType) {
		return SqmFrom.super.join( entityJavaType, joinType );
	}

	@Override
	<Y> JpaJoin<R, Y> join(EntityType<Y> entity);

	@Override
	<Y> JpaJoin<R, Y> join(EntityType<Y> entity, JoinType joinType);

	@Override
	<X> JpaEntityJoin<R, X> join(EntityDomainType<X> entity);

	@Override
	<X> JpaEntityJoin<R, X> join(EntityDomainType<X> entity, SqmJoinType joinType);

	@Override
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery);

	@Override
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType);

	@Override
	<X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery);

	@Override
	<X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery, SqmJoinType joinType);

	@Override
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType, boolean lateral);

	@Override
	<X> JpaJoin<?, X> join(JpaCteCriteria<X> cte);

	@Override
	<X> JpaJoin<?, X> join(JpaCteCriteria<X> cte, SqmJoinType joinType);

	@Override
	<X> JpaCrossJoin<X> crossJoin(Class<X> entityJavaType);

	@Override
	<X> JpaCrossJoin<X> crossJoin(EntityDomainType<X> entity);

	@Override
	<A> SqmSingularJoin<R, A> join(SingularAttribute<? super R, A> attribute);

	@Override
	<A> SqmSingularJoin<R, A> join(SingularAttribute<? super R, A> attribute, JoinType jt);

	@Override
	<E> SqmBagJoin<R, E> join(CollectionAttribute<? super R, E> attribute);

	@Override
	<E> SqmBagJoin<R, E> join(CollectionAttribute<? super R, E> attribute, JoinType jt);

	@Override
	<E> SqmSetJoin<R, E> join(SetAttribute<? super R, E> set);

	@Override
	<E> SqmSetJoin<R, E> join(SetAttribute<? super R, E> set, JoinType jt);

	@Override
	<E> SqmListJoin<R, E> join(ListAttribute<? super R, E> list);

	@Override
	<E> SqmListJoin<R, E> join(ListAttribute<? super R, E> list, JoinType jt);

	@Override
	<K, V> SqmMapJoin<R, K, V> join(MapAttribute<? super R, K, V> map);

	@Override
	<K, V> SqmMapJoin<R, K, V> join(MapAttribute<? super R, K, V> map, JoinType jt);

	@Override
	<X, Y> SqmBagJoin<X, Y> joinCollection(String attributeName);

	@Override
	<X, Y> SqmBagJoin<X, Y> joinCollection(String attributeName, JoinType jt);

	@Override
	<X, Y> SqmSetJoin<X, Y> joinSet(String attributeName);

	@Override
	<X, Y> SqmSetJoin<X, Y> joinSet(String attributeName, JoinType jt);

	@Override
	<X, Y> SqmListJoin<X, Y> joinList(String attributeName);

	@Override
	<X, Y> SqmListJoin<X, Y> joinList(String attributeName, JoinType jt);

	@Override
	<X, K, V> SqmMapJoin<X, K, V> joinMap(String attributeName);

	@Override
	<X, K, V> SqmMapJoin<X, K, V> joinMap(String attributeName, JoinType jt);
}

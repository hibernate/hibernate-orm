/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaFrom;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedFrom;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Models a SqmPathSource's inclusion in the {@code FROM} clause.
 *
 * @param <L> The from-element's "left hand side".  It may be the same as {@code R} for roots.
 * @param <R> The from-element's "right hand side".  For joins, this is the target side.
 *
 * @author Steve Ebersole
 */
public interface SqmFrom<L, R> extends SqmVisitableNode, SqmPath<R>, JpaFrom<L, R> {
	/**
	 * The Navigable for an SqmFrom will always be a NavigableContainer
	 *
	 * {@inheritDoc}
	 * @return
	 */
	@Override
	SqmPathSource<R> getReferencedPathSource();

	/**
	 * Retrieve the explicit alias, if one, otherwise return a generated one.
	 */
	default String resolveAlias(SqmRenderContext context) {
		return context.resolveAlias( this );
	}

	boolean hasJoins();

	int getNumberOfJoins();

	/**
	 * The joins associated with this SqmFrom
	 */
	List<SqmJoin<R,?>> getSqmJoins();

	/**
	 * Add an associated join
	 */
	void addSqmJoin(SqmJoin<R, ?> join);

	/**
	 * Visit all associated joins
	 */
	void visitSqmJoins(Consumer<SqmJoin<R, ?>> consumer);

	/**
	 * The treats associated with this SqmFrom
	 */
	List<SqmTreatedFrom<?,?,@Nullable ?>> getSqmTreats();

	default boolean hasTreats() {
		return !isEmpty( getSqmTreats() );
	}

	@Override
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(Class<S> treatJavaType, @Nullable String alias);

	@Override
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias);

	// Since equals only does "syntactic" equality to understand if a node in an expression and predicate is equal,
	// also define a method to do deep equality checking for the SqmFromClause
	default boolean deepEquals(SqmFrom<?, ?> object) {
		return equals( object );
	}

	// Since isCompatible only does "syntactic" equality to understand if a node in an expression and predicate is compatible,
	// also define a method to do deep compatibility checking for the SqmFromClause
	default boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return isCompatible( object );
	}

	static boolean areDeepEqual(List<? extends SqmFrom<?, ?>> theseFroms, List<? extends SqmFrom<?, ?>> thoseFroms) {
		if ( theseFroms.size() != thoseFroms.size() ) {
			return false;
		}
		for ( int i = 0; i < theseFroms.size(); i++ ) {
			if ( !theseFroms.get( i ).deepEquals( thoseFroms.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	static boolean areDeepCompatible(List<? extends SqmFrom<?, ?>> theseFroms, List<? extends SqmFrom<?, ?>> thoseFroms) {
		if ( theseFroms.size() != thoseFroms.size() ) {
			return false;
		}
		for ( int i = 0; i < theseFroms.size(); i++ ) {
			if ( !theseFroms.get( i ).isDeepCompatible( thoseFroms.get( i ) ) ) {
				return false;
			}
		}
		return true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	SqmFrom<L, R> getCorrelationParent();

	@Override
	<Y> SqmEntityJoin<R, Y> join(Class<Y> entityClass);

	@Override
	<Y> SqmEntityJoin<R, Y> join(Class<Y> entityClass, JoinType joinType);

	@Incubating
	boolean hasImplicitlySelectableJoin();

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
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName, JoinType jt);

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

	@Override
	SqmFrom<L, R> copy(SqmCopyContext context);
}

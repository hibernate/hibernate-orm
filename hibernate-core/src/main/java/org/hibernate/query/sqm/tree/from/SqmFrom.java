/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

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
	List<SqmTreatedFrom<?,?,@org.checkerframework.checker.nullness.qual.Nullable ?>> getSqmTreats();

	default boolean hasTreats() {
		return !isEmpty( getSqmTreats() );
	}

	@Nonnull
	@Override
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(@Nonnull Class<S> treatJavaType);

	@Nonnull
	@Override
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatTarget);

	@Override
	@Nonnull
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Override
	@Nonnull
	<S extends R> SqmTreatedFrom<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);

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

	@Nonnull
	@Override
	SqmFrom<L, R> getCorrelationParent();

	@Nonnull
	@Override
	<Y> SqmEntityJoin<R, Y> join(@Nonnull Class<Y> entityClass);

	@Nonnull
	@Override
	<Y> SqmEntityJoin<R, Y> join(@Nonnull Class<Y> entityClass, @Nonnull JoinType joinType);

	@Incubating
	boolean hasImplicitlySelectableJoin();

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
	<Y> SqmAttributeJoin<R, Y> join(@Nonnull String attributeName);

	@Nonnull
	@Override
	<Y> SqmAttributeJoin<R, Y> join(@Nonnull String attributeName, @Nonnull JoinType jt);

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

	@Override
	SqmFrom<L, R> copy(SqmCopyContext context);
}

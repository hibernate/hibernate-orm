/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Collection;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.SqmJoinType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * API extension to the JPA {@link From} contract
 *
 * @author Steve Ebersole
 */
public interface JpaFrom<O,T> extends JpaPath<T>, JpaFetchParent<O,T>, From<O,T> {
	@Override
	JpaFrom<O,T> getCorrelationParent();

	@Override
	<Y> JpaEntityJoin<T, Y> join(Class<Y> entityClass);

	@Override
	<Y> JpaEntityJoin<T, Y> join(Class<Y> entityClass, JoinType joinType);

	default <X> JpaEntityJoin<T, X> join(Class<X> entityJavaType, SqmJoinType joinType) {
		return join( entityJavaType, joinType.getCorrespondingJpaJoinType() );
	}

	@Override
	<Y> JpaJoin<T, Y> join(EntityType<Y> entity);

	@Override
	<Y> JpaJoin<T, Y> join(EntityType<Y> entity, JoinType joinType);

	<X> JpaEntityJoin<T,X> join(EntityDomainType<X> entity);

	<X> JpaEntityJoin<T,X> join(EntityDomainType<X> entity, SqmJoinType joinType);

	@Incubating
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery);

	@Incubating
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType);

	@Incubating
	<X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery);

	@Incubating
	<X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery, SqmJoinType joinType);

	@Incubating
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType, boolean lateral);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #join(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> join(JpaSetReturningFunction<X> function);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType, boolean)} passing {@code false}
	 * for the {@code lateral} parameter.
	 *
	 * @see #join(JpaSetReturningFunction, SqmJoinType, boolean)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> join(JpaSetReturningFunction<X> function, SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinLateral(JpaSetReturningFunction, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinLateral(JpaSetReturningFunction<X> function);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType, boolean)} passing {@code true}
	 * for the {@code lateral} parameter.
	 *
	 * @see #join(JpaSetReturningFunction, SqmJoinType, boolean)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinLateral(JpaSetReturningFunction<X> function, SqmJoinType joinType);

	/**
	 * Creates and returns a join node for the given set returning function.
	 * If function arguments refer to correlated paths, the {@code lateral} argument must be set to {@code true}.
	 * Failing to do so when necessary may lead to an error during query compilation or execution.
	 *
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> join(JpaSetReturningFunction<X> function, SqmJoinType joinType, boolean lateral);

	/**
	 * Like calling the overload {@link #joinArray(String, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArray(String, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArray(String arrayAttributeName);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestArray(Expression)}
	 * with the result of {@link #get(String)} passing the given attribute name.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArray(String arrayAttributeName, SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinArray(SingularAttribute, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArray(SingularAttribute, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArray(SingularAttribute<? super T, X[]> arrayAttribute);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestArray(Expression)}
	 * with the given attribute.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArray(SingularAttribute<? super T, X[]> arrayAttribute, SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinArrayCollection(String, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArrayCollection(String, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArrayCollection(String collectionAttributeName);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestCollection(Expression)}
	 * with the result of {@link #get(String)} passing the given attribute name.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArrayCollection(String collectionAttributeName, SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinArrayCollection(SingularAttribute, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArrayCollection(SingularAttribute, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArrayCollection(SingularAttribute<? super T, ? extends Collection<X>> collectionAttribute);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestCollection(Expression)}
	 * with the given attribute.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	<X> JpaFunctionJoin<X> joinArrayCollection(SingularAttribute<? super T, ? extends Collection<X>> collectionAttribute, SqmJoinType joinType);

	@Incubating
	<X> JpaJoin<?, X> join(JpaCteCriteria<X> cte);

	@Incubating
	<X> JpaJoin<?, X> join(JpaCteCriteria<X> cte, SqmJoinType joinType);

	@Incubating
	<X> JpaCrossJoin<X> crossJoin(Class<X> entityJavaType);

	@Incubating
	<X> JpaCrossJoin<X> crossJoin(EntityDomainType<X> entity);

	// Covariant overrides

	@Override
	<Y> JpaJoin<T, Y> join(SingularAttribute<? super T, Y> attribute);

	@Override
	<Y> JpaJoin<T, Y> join(SingularAttribute<? super T, Y> attribute, JoinType jt);

	@Override
	<Y> JpaCollectionJoin<T, Y> join(CollectionAttribute<? super T, Y> collection);

	@Override
	<Y> JpaSetJoin<T, Y> join(SetAttribute<? super T, Y> set);

	@Override
	<Y> JpaListJoin<T, Y> join(ListAttribute<? super T, Y> list);

	@Override
	<K, V> JpaMapJoin<T, K, V> join(MapAttribute<? super T, K, V> map);

	@Override
	<Y> JpaCollectionJoin<T, Y> join(CollectionAttribute<? super T, Y> collection, JoinType jt);

	@Override
	<Y> JpaSetJoin<T, Y> join(SetAttribute<? super T, Y> set, JoinType jt);

	@Override
	<Y> JpaListJoin<T, Y> join(ListAttribute<? super T, Y> list, JoinType jt);

	@Override
	<K, V> JpaMapJoin<T, K, V> join(MapAttribute<? super T, K, V> map, JoinType jt);

	@Override
	<X, Y> JpaJoin<X, Y> join(String attributeName);

	@Override
	<X, Y> JpaCollectionJoin<X, Y> joinCollection(String attributeName);

	@Override
	<X, Y> JpaSetJoin<X, Y> joinSet(String attributeName);

	@Override
	<X, Y> JpaListJoin<X, Y> joinList(String attributeName);

	@Override
	<X, K, V> JpaMapJoin<X, K, V> joinMap(String attributeName);

	@Override
	<X, Y> JpaJoin<X, Y> join(String attributeName, JoinType jt);

	@Override
	<X, Y> JpaCollectionJoin<X, Y> joinCollection(String attributeName, JoinType jt);

	@Override
	<X, Y> JpaSetJoin<X, Y> joinSet(String attributeName, JoinType jt);

	@Override
	<X, Y> JpaListJoin<X, Y> joinList(String attributeName, JoinType jt);

	@Override
	<X, K, V> JpaMapJoin<X, K, V> joinMap(String attributeName, JoinType jt);

	@Override
	<S extends T> JpaTreatedFrom<O,T,S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends T> JpaTreatedFrom<O,T,S> treatAs(EntityDomainType<S> treatJavaType);
}

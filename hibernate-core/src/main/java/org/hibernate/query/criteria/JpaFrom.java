/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Collection;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;

import jakarta.annotation.Nonnull;
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
	/**
	 * Return the correlation parent of this from element.
	 */
	@Nonnull
	@Override
	JpaFrom<O,T> getCorrelationParent();

	/**
	 * Create an entity join.
	 */
	@Nonnull
	@Override
	<Y> JpaEntityJoin<T, Y> join(@Nonnull Class<Y> entityClass);

	/**
	 * Create an entity join.
	 */
	@Nonnull
	@Override
	<Y> JpaEntityJoin<T, Y> join(@Nonnull Class<Y> entityClass, @Nonnull JoinType joinType);

	/**
	 * Create an entity join.
	 */
	@Nonnull
	<X> JpaEntityJoin<T, X> join(@Nonnull Class<X> entityJavaType, @Nonnull org.hibernate.query.common.JoinType joinType);

	/**
	 * Create an entity join.
	 */
	@Nonnull
	@Override
	<Y> JpaJoin<T, Y> join(@Nonnull EntityType<Y> entity);

	/**
	 * Create an entity join.
	 */
	@Nonnull
	@Override
	<Y> JpaJoin<T, Y> join(@Nonnull EntityType<Y> entity, @Nonnull JoinType joinType);

	/**
	 * Create an entity join.
	 */
	@Nonnull
	<X> JpaEntityJoin<T,X> join(@Nonnull EntityDomainType<X> entity);

	/**
	 * Create an entity join.
	 */
	@Nonnull
	<X> JpaEntityJoin<T,X> join(@Nonnull EntityDomainType<X> entity, @Nonnull org.hibernate.query.common.JoinType joinType);

	/**
	 * Create a derived join for the given subquery.
	 */
	@Incubating
	@Nonnull
	<X> JpaDerivedJoin<X> join(@Nonnull Subquery<X> subquery);

	/**
	 * Create a derived join for the given subquery.
	 */
	<X> JpaDerivedJoin<X> join(@Nonnull Subquery<X> subquery, @Nonnull org.hibernate.query.common.JoinType joinType);

	/**
	 * Create a lateral derived join for the given subquery.
	 */
	@Incubating
	@Nonnull
	<X> JpaDerivedJoin<X> joinLateral(@Nonnull Subquery<X> subquery);

	/**
	 * Create a lateral derived join for the given subquery.
	 */
	@Nonnull
	<X> JpaDerivedJoin<X> joinLateral(@Nonnull Subquery<X> subquery, @Nonnull org.hibernate.query.common.JoinType joinType);

	/**
	 * Create a derived join for the given subquery.
	 */
	@Nonnull
	<X> JpaDerivedJoin<X> join(@Nonnull Subquery<X> subquery, @Nonnull org.hibernate.query.common.JoinType joinType, boolean lateral);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #join(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> join(@Nonnull JpaSetReturningFunction<X> function);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType, boolean)} passing {@code false}
	 * for the {@code lateral} parameter.
	 *
	 * @see #join(JpaSetReturningFunction, SqmJoinType, boolean)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> join(@Nonnull JpaSetReturningFunction<X> function, @Nonnull SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinLateral(JpaSetReturningFunction, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinLateral(@Nonnull JpaSetReturningFunction<X> function);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType, boolean)} passing {@code true}
	 * for the {@code lateral} parameter.
	 *
	 * @see #join(JpaSetReturningFunction, SqmJoinType, boolean)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinLateral(@Nonnull JpaSetReturningFunction<X> function, @Nonnull SqmJoinType joinType);

	/**
	 * Creates and returns a join node for the given set returning function.
	 * If function arguments refer to correlated paths, the {@code lateral} argument must be set to {@code true}.
	 * Failing to do so when necessary may lead to an error during query compilation or execution.
	 *
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> join(@Nonnull JpaSetReturningFunction<X> function, @Nonnull SqmJoinType joinType, boolean lateral);

	/**
	 * Like calling the overload {@link #joinArray(String, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArray(String, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArray(@Nonnull String arrayAttributeName);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestArray(Expression)}
	 * with the result of {@link #get(String)} passing the given attribute name.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArray(@Nonnull String arrayAttributeName, @Nonnull SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinArray(SingularAttribute, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArray(SingularAttribute, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArray(@Nonnull SingularAttribute<? super T, X[]> arrayAttribute);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestArray(Expression)}
	 * with the given attribute.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArray(@Nonnull SingularAttribute<? super T, X[]> arrayAttribute, @Nonnull SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinArrayCollection(String, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArrayCollection(String, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArrayCollection(@Nonnull String collectionAttributeName);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestCollection(Expression)}
	 * with the result of {@link #get(String)} passing the given attribute name.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArrayCollection(@Nonnull String collectionAttributeName, @Nonnull SqmJoinType joinType);

	/**
	 * Like calling the overload {@link #joinArrayCollection(SingularAttribute, SqmJoinType)} with {@link SqmJoinType#INNER}.
	 *
	 * @see #joinArrayCollection(SingularAttribute, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArrayCollection(@Nonnull SingularAttribute<? super T, ? extends Collection<X>> collectionAttribute);

	/**
	 * Like calling the overload {@link #join(JpaSetReturningFunction, SqmJoinType)} with {@link HibernateCriteriaBuilder#unnestCollection(Expression)}
	 * with the given attribute.
	 *
	 * @see #joinLateral(JpaSetReturningFunction, SqmJoinType)
	 * @since 7.0
	 */
	@Incubating
	@Nonnull
	<X> JpaFunctionJoin<X> joinArrayCollection(@Nonnull SingularAttribute<? super T, ? extends Collection<X>> collectionAttribute, @Nonnull SqmJoinType joinType);

	/**
	 * Create a join to the given CTE.
	 */
	@Incubating
	@Nonnull
	<X> JpaJoin<?, X> join(@Nonnull JpaCteCriteria<X> cte);

	/**
	 * Create a join to the given CTE.
	 */
	@Nonnull
	<X> JpaJoin<?, X> join(@Nonnull JpaCteCriteria<X> cte, @Nonnull org.hibernate.query.common.JoinType joinType);

	/**
	 * Create a cross join to the given entity type.
	 */
	@Incubating
	@Nonnull
	<X> JpaCrossJoin<T, X> crossJoin(@Nonnull Class<X> entityJavaType);

	/**
	 * Create a cross join to the given entity type.
	 */
	@Incubating
	@Nonnull
	<X> JpaCrossJoin<T, X> crossJoin(@Nonnull EntityDomainType<X> entity);

	// Covariant overrides

	/**
	 * Create a join to the given singular attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaJoin<T, Y> join(@Nonnull SingularAttribute<? super T, Y> attribute);

	/**
	 * Create a join to the given singular attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaJoin<T, Y> join(@Nonnull SingularAttribute<? super T, Y> attribute, @Nonnull JoinType jt);

	/**
	 * Create a collection join to the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaCollectionJoin<T, Y> join(@Nonnull CollectionAttribute<? super T, Y> collection);

	/**
	 * Create a set join to the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaSetJoin<T, Y> join(@Nonnull SetAttribute<? super T, Y> set);

	/**
	 * Create a list join to the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaListJoin<T, Y> join(@Nonnull ListAttribute<? super T, Y> list);

	/**
	 * Create a map join to the given attribute.
	 */
	@Nonnull
	@Override
	<K, V> JpaMapJoin<T, K, V> join(@Nonnull MapAttribute<? super T, K, V> map);

	/**
	 * Create a collection join to the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaCollectionJoin<T, Y> join(@Nonnull CollectionAttribute<? super T, Y> collection, @Nonnull JoinType jt);

	/**
	 * Create a set join to the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaSetJoin<T, Y> join(@Nonnull SetAttribute<? super T, Y> set, @Nonnull JoinType jt);

	/**
	 * Create a list join to the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaListJoin<T, Y> join(@Nonnull ListAttribute<? super T, Y> list, @Nonnull JoinType jt);

	/**
	 * Create a map join to the given attribute.
	 */
	@Nonnull
	@Override
	<K, V> JpaMapJoin<T, K, V> join(@Nonnull MapAttribute<? super T, K, V> map, @Nonnull JoinType jt);

	/**
	 * Create a join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaJoin<T, Y> join(@Nonnull String attributeName);

	/**
	 * Create a collection join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaCollectionJoin<T, Y> joinCollection(@Nonnull String attributeName);

	/**
	 * Create a set join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaSetJoin<T, Y> joinSet(@Nonnull String attributeName);

	/**
	 * Create a list join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaListJoin<T, Y> joinList(@Nonnull String attributeName);

	/**
	 * Create a map join to the named attribute.
	 */
	@Nonnull
	@Override
	<K, V> JpaMapJoin<T, K, V> joinMap(@Nonnull String attributeName);

	/**
	 * Create a join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaJoin<T, Y> join(@Nonnull String attributeName, @Nonnull JoinType jt);

	/**
	 * Create a collection join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaCollectionJoin<T, Y> joinCollection(@Nonnull String attributeName, @Nonnull JoinType jt);

	/**
	 * Create a set join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaSetJoin<T, Y> joinSet(@Nonnull String attributeName, @Nonnull JoinType jt);

	/**
	 * Create a list join to the named attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaListJoin<T, Y> joinList(@Nonnull String attributeName, @Nonnull JoinType jt);

	/**
	 * Create a map join to the named attribute.
	 */
	@Nonnull
	@Override
	<K, V> JpaMapJoin<T, K, V> joinMap(@Nonnull String attributeName, @Nonnull JoinType jt);

	/**
	 * Downcast this from element to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends T> JpaTreatedFrom<O,T,S> treatAs(@Nonnull Class<S> treatJavaType);

	/**
	 * Downcast this from element to the specified subtype.
	 */
	@Nonnull
	@Override
	default <S extends T> JpaFrom<?, S> treat(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType );
	}

	/**
	 * Downcast this from element to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends T> JpaTreatedFrom<O,T,S> treatAs(@Nonnull EntityDomainType<S> treatJavaType);

	/**
	 * Create an expression for the identifier of this from element.
	 */
	@Incubating
	@Nonnull
	JpaExpression<?> id();
}

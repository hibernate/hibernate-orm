/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.SqmJoinType;

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

	/**
	 * @deprecated This method is a layer-breaker, leaking the SQM type
	 *             {@link SqmJoinType} onto an API. It will be removed.
	 *             Use {@link #join(Class, org.hibernate.query.common.JoinType)}
	 */
	@Deprecated(since="7", forRemoval = true)
	default <X> JpaEntityJoin<T, X> join(Class<X> entityJavaType, SqmJoinType joinType) {
		return join( entityJavaType, joinType.getCorrespondingJpaJoinType() );
	}

	default <X> JpaEntityJoin<T, X> join(Class<X> entityJavaType, org.hibernate.query.common.JoinType joinType) {
		return join( entityJavaType, SqmJoinType.from(joinType) );
	}

	@Override
	<Y> JpaJoin<T, Y> join(EntityType<Y> entity);

	@Override
	<Y> JpaJoin<T, Y> join(EntityType<Y> entity, JoinType joinType);

	<X> JpaEntityJoin<T,X> join(EntityDomainType<X> entity);

	/**
	 * @deprecated This method is a layer-breaker, leaking the SQM type
	 *             {@link SqmJoinType} onto an API. It will be removed.
	 *             Use {@link #join(EntityDomainType, org.hibernate.query.common.JoinType)}
	 */
	@Deprecated(since = "7", forRemoval = true)
	<X> JpaEntityJoin<T,X> join(EntityDomainType<X> entity, SqmJoinType joinType);

	default <X> JpaEntityJoin<T,X> join(EntityDomainType<X> entity, org.hibernate.query.common.JoinType joinType) {
		return join( entity, SqmJoinType.from(joinType) );
	}

	@Incubating
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery);

	/**
	 * @deprecated This method is a layer-breaker, leaking the SQM type
	 *             {@link SqmJoinType} onto an API. It will be removed.
	 *             Use {@link #join(Subquery, org.hibernate.query.common.JoinType)}
	 */
	@Incubating @Deprecated(since = "7", forRemoval = true)
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType);

	default <X> JpaDerivedJoin<X> join(Subquery<X> subquery, org.hibernate.query.common.JoinType joinType) {
		return join( subquery, SqmJoinType.from(joinType) );
	}

	@Incubating
	<X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery);

	/**
	 * @deprecated This method is a layer-breaker, leaking the SQM type
	 *             {@link SqmJoinType} onto an API. It will be removed.
	 *             Use {@link #joinLateral(Subquery, org.hibernate.query.common.JoinType)}
	 */
	@Incubating @Deprecated(since = "7", forRemoval = true)
	<X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery, SqmJoinType joinType);

	default <X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery, org.hibernate.query.common.JoinType joinType) {
		return joinLateral( subquery, SqmJoinType.from(joinType) );
	}

	/**
	 * @deprecated This method is a layer-breaker, leaking the SQM type
	 *             {@link SqmJoinType} onto an API. It will be removed.
	 *             Use {@link #join(Subquery, org.hibernate.query.common.JoinType, boolean)}
	 */
	@Incubating @Deprecated(since = "7", forRemoval = true)
	<X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType, boolean lateral);

	default <X> JpaDerivedJoin<X> join(Subquery<X> subquery, org.hibernate.query.common.JoinType joinType, boolean lateral) {
		return join( subquery, SqmJoinType.from(joinType), lateral );
	}

	@Incubating
	<X> JpaJoin<?, X> join(JpaCteCriteria<X> cte);

	/**
	 * @deprecated This method is a layer-breaker, leaking the SQM type
	 *             {@link SqmJoinType} onto an API. It will be removed.
	 *             Use {@link #join(JpaCteCriteria, org.hibernate.query.common.JoinType)}
	 */
	@Incubating @Deprecated(since = "7", forRemoval = true)
	<X> JpaJoin<?, X> join(JpaCteCriteria<X> cte, SqmJoinType joinType);

	default <X> JpaJoin<?, X> join(JpaCteCriteria<X> cte, org.hibernate.query.common.JoinType joinType) {
		return join( cte, SqmJoinType.from(joinType) );
	}

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

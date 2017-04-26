/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.from;

import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPath;

/**
 * @author Steve Ebersole
 */
public interface JpaFrom<Z,X> extends From<Z,X>, JpaPath<X>, JpaFetchParent<Z,X> {
	//	FromImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);
	void prepareCorrelationDelegate(JpaFrom<Z, X> parent);
	JpaFrom<Z, X> getCorrelationParent();

	@Override
	<Y> JpaAttributeJoin<X, Y> join(SingularAttribute<? super X, Y> attribute);

	@Override
	<Y> JpaAttributeJoin<X, Y> join(SingularAttribute<? super X, Y> attribute, JoinType jt);

	@Override
	<Y> JpaCollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection);

	@Override
	<Y> JpaSetJoin<X, Y> join(SetAttribute<? super X, Y> set);

	@Override
	<Y> JpaListJoin<X, Y> join(ListAttribute<? super X, Y> list);

	@Override
	<K, V> JpaMapJoin<X, K, V> join(MapAttribute<? super X, K, V> map);

	@Override
	<Y> JpaCollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection, JoinType jt);

	@Override
	<Y> JpaSetJoin<X, Y> join(SetAttribute<? super X, Y> set, JoinType jt);

	@Override
	<Y> JpaListJoin<X, Y> join(ListAttribute<? super X, Y> list, JoinType jt);

	@Override
	<K, V> JpaMapJoin<X, K, V> join(MapAttribute<? super X, K, V> map, JoinType jt);

	@Override
	<X1, Y> JpaAttributeJoin<X1, Y> join(String attributeName);

	@Override
	<X1, Y> JpaCollectionJoin<X1, Y> joinCollection(String attributeName);

	@Override
	<X1, Y> JpaSetJoin<X1, Y> joinSet(String attributeName);

	@Override
	<X1, Y> JpaListJoin<X1, Y> joinList(String attributeName);

	@Override
	<X1, K, V> JpaMapJoin<X1, K, V> joinMap(String attributeName);

	@Override
	<X1, Y> JpaAttributeJoin<X1, Y> join(String attributeName, JoinType jt);

	@Override
	<X1, Y> JpaCollectionJoin<X1, Y> joinCollection(String attributeName, JoinType jt);

	@Override
	<X1, Y> JpaSetJoin<X1, Y> joinSet(String attributeName, JoinType jt);

	@Override
	<X1, Y> JpaListJoin<X1, Y> joinList(String attributeName, JoinType jt);

	@Override
	<X1, K, V> JpaMapJoin<X1, K, V> joinMap(String attributeName, JoinType jt);
}

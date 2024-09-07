/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.query.common.FetchClauseType;

/**
 * @author Steve Ebersole
 */
public interface JpaSubQuery<T> extends Subquery<T>, JpaSelectCriteria<T>, JpaCriteriaSelect<T>, JpaExpression<T>, JpaCteContainer {

	JpaSubQuery<T> multiselect(Selection<?>... selections);

	JpaSubQuery<T> multiselect(List<Selection<?>> selectionList);

	<X> JpaCrossJoin<X> correlate(JpaCrossJoin<X> parentCrossJoin);

	<X> JpaEntityJoin<T,X> correlate(JpaEntityJoin<T,X> parentEntityJoin);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	JpaExpression<Number> getOffset();

	JpaSubQuery<T> offset(JpaExpression<? extends Number> offset);

	JpaSubQuery<T> offset(Number offset);

	JpaExpression<Number> getFetch();

	JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch);

	JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	JpaSubQuery<T> fetch(Number fetch);

	JpaSubQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Order by clause

	List<JpaOrder> getOrderList();

	JpaSubQuery<T> orderBy(Order... o);

	JpaSubQuery<T> orderBy(List<Order> o);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	JpaSubQuery<T> distinct(boolean distinct);

	@Override
	JpaExpression<T> getSelection();

	@Override
	JpaSubQuery<T> select(Expression<T> expression);

	@Override
	JpaSubQuery<T> where(Expression<Boolean> restriction);

	@Override
	JpaSubQuery<T> where(Predicate... restrictions);

	@Override
	JpaSubQuery<T> groupBy(Expression<?>... grouping);

	@Override
	JpaSubQuery<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaSubQuery<T> having(Expression<Boolean> restriction);

	@Override
	JpaSubQuery<T> having(Predicate... restrictions);

	@Override
	<Y> JpaRoot<Y> correlate(Root<Y> parentRoot);

	@Override
	<X, Y> JpaJoin<X, Y> correlate(Join<X, Y> parentJoin);

	@Override
	<X, Y> JpaCollectionJoin<X, Y> correlate(CollectionJoin<X, Y> parentCollection);

	@Override
	<X, Y> JpaSetJoin<X, Y> correlate(SetJoin<X, Y> parentSet);

	@Override
	<X, Y> JpaListJoin<X, Y> correlate(ListJoin<X, Y> parentList);

	@Override
	<X, K, V> JpaMapJoin<X, K, V> correlate(MapJoin<X, K, V> parentMap);
}

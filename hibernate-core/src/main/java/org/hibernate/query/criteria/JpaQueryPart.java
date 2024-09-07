/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.query.common.FetchClauseType;

/**
 * Models a query part i.e. the commonalities between a query group and a query specification.
 *
 * @see JpaQueryStructure
 * @see JpaQueryGroup
 *
 * @author Christian Beikov
 */
public interface JpaQueryPart<T> extends JpaCriteriaNode {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering clause

	List<? extends JpaOrder> getSortSpecifications();

	JpaQueryPart<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	//TODO: these operations should only accept integer literals or parameters

	JpaExpression<? extends Number> getOffset();

	JpaQueryPart<T> setOffset(JpaExpression<? extends Number> offset);

	JpaExpression<? extends Number> getFetch();

	JpaQueryPart<T> setFetch(JpaExpression<? extends Number> fetch);

	JpaQueryPart<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.query.sqm.FetchClauseType;

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

	JpaExpression<?> getOffset();

	JpaQueryPart<T> setOffset(JpaExpression<?> offset);

	JpaExpression<?> getFetch();

	JpaQueryPart<T> setFetch(JpaExpression<?> fetch);

	JpaQueryPart<T> setFetch(JpaExpression<?> fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
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

	@Nullable JpaExpression<? extends Number> getOffset();

	JpaQueryPart<T> setOffset(@Nullable JpaExpression<? extends Number> offset);

	@Nullable JpaExpression<? extends Number> getFetch();

	JpaQueryPart<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	JpaQueryPart<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();
}

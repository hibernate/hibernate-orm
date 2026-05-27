/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

	@Nonnull
	List<? extends JpaOrder> getSortSpecifications();

	@Nonnull
	JpaQueryPart<T> setSortSpecifications(@Nonnull List<? extends JpaOrder> sortSpecifications);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	//TODO: these operations should only accept integer literals or parameters

	@Nullable
	JpaExpression<? extends Number> getOffset();

	@Nonnull
	JpaQueryPart<T> setOffset(JpaExpression<? extends Number> offset);

	@Nullable
	JpaExpression<? extends Number> getFetch();

	@Nonnull
	JpaQueryPart<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	@Nonnull
	JpaQueryPart<T> setFetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();
}

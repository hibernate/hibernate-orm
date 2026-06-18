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
 * A query group i.e. query parts connected with a set operator.
 *
 * @author Christian Beikov
 */
public interface JpaQueryGroup<T> extends JpaQueryPart<T> {

	/**
	 * Return the query parts of this query group.
	 */
	@Nonnull
	List<? extends JpaQueryPart<T>> getQueryParts();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	/**
	 * Set the ordering specifications.
	 */
	@Override
	@Nonnull
	JpaQueryGroup<T> setSortSpecifications(@Nonnull List<? extends JpaOrder> sortSpecifications);

	/**
	 * Set the query offset.
	 */
	@Override
	@Nonnull
	JpaQueryGroup<T> setOffset(@Nonnull JpaExpression<? extends Number> offset);

	/**
	 * Set the query fetch limit.
	 */
	@Override
	@Nonnull
	JpaQueryGroup<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	/**
	 * Set the query fetch limit.
	 */
	@Override
	@Nonnull
	JpaQueryGroup<T> setFetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

}

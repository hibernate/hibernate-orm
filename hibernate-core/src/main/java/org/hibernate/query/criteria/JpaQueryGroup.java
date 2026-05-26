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

	@Nonnull
	List<? extends JpaQueryPart<T>> getQueryParts();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	@Nonnull
	JpaQueryGroup<T> setSortSpecifications(@Nonnull List<? extends JpaOrder> sortSpecifications);

	@Override
	@Nonnull
	JpaQueryGroup<T> setOffset(@Nonnull JpaExpression<? extends Number> offset);

	@Override
	@Nonnull
	JpaQueryGroup<T> setFetch(@Nullable JpaExpression<? extends Number> fetch);

	@Override
	@Nonnull
	JpaQueryGroup<T> setFetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

}

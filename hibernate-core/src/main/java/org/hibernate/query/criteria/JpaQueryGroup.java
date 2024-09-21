/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.SetOperator;

/**
 * A query group i.e. query parts connected with a set operator.
 *
 * @author Christian Beikov
 */
public interface JpaQueryGroup<T> extends JpaQueryPart<T> {

	List<? extends JpaQueryPart<T>> getQueryParts();

	SetOperator getSetOperator();

	void setSetOperator(SetOperator setOperator);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	JpaQueryGroup<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);

	@Override
	JpaQueryGroup<T> setOffset(JpaExpression<? extends Number> offset);

	@Override
	JpaQueryGroup<T> setFetch(JpaExpression<? extends Number> fetch);

	@Override
	JpaQueryGroup<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

}

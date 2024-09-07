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
 * A query group i.e. query parts connected with a set operator.
 *
 * @author Christian Beikov
 */
public interface JpaQueryGroup<T> extends JpaQueryPart<T> {

	List<? extends JpaQueryPart<T>> getQueryParts();

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

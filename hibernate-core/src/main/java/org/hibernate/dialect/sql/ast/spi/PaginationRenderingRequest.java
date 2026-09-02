/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.query.select.QueryPart;

/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// pagination for one query part.
///
/// Implementations of [PaginationRenderingSupport] should inspect these facts
/// and return a [PaginationRenderingPlan]. They should not mutate the query part
/// or retain this request after [PaginationRenderingSupport#determinePlan].
///
/// @since 8.0
/// @author Steve Ebersole
public interface PaginationRenderingRequest {
	/// The query part whose pagination clause is being rendered.
	QueryPart queryPart();

	/// Whether pagination originates in execution-time query options instead of
	/// an explicit SQL AST fetch or offset clause.
	boolean usesQueryOptionsLimit();

	/// Whether this query part has an effective offset.
	boolean hasOffset();

	/// Whether this query part has an effective fetch limit.
	boolean hasFetch();

	/// The semantic kind of fetch clause, including whether ties or percentages
	/// are requested.
	FetchClauseType fetchClauseType();

	/// Whether row-numbering emulation is currently being rendered for this same
	/// query part.
	boolean isRowNumberingCurrentQueryPart();

	/// Whether this query part is nested in a query group and therefore may have
	/// stricter clause-placement rules.
	boolean isNestedInQueryGroup();
}

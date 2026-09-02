/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;

/// Reusable pagination-plan selection for SQL Server syntax families.
///
/// @since 8.0
/// @author Steve Ebersole
public final class SQLServerPaginationRenderingSupport implements PaginationRenderingSupport {
	public static final SQLServerPaginationRenderingSupport MODERN =
			new SQLServerPaginationRenderingSupport( true, true );

	private final boolean supportsOffsetInTop;
	private final boolean supportsOffsetFetchClause;

	public SQLServerPaginationRenderingSupport(
			boolean supportsOffsetInTop,
			boolean supportsOffsetFetchClause) {
		this.supportsOffsetInTop = supportsOffsetInTop;
		this.supportsOffsetFetchClause = supportsOffsetFetchClause;
	}

	@Override
	public PaginationRenderingPlan determinePlan(PaginationRenderingRequest request) {
		if ( !request.hasOffset() && !request.hasFetch() ) {
			return new PaginationRenderingPlan.None();
		}

		final boolean rowsOnly = request.fetchClauseType() == null
				|| request.fetchClauseType() == FetchClauseType.ROWS_ONLY;
		if ( request.queryPart() instanceof QueryGroup ) {
			return supportsOffsetFetchClause && rowsOnly
					? new PaginationRenderingPlan.OffsetFetch( true )
					: new PaginationRenderingPlan.Window( !rowsOnly );
		}

		if ( !supportsOffsetInTop || !request.hasOffset() ) {
			return new PaginationRenderingPlan.Top( true, true );
		}
		final QuerySpec querySpec = (QuerySpec) request.queryPart();
		if ( !supportsOffsetFetchClause
				|| !rowsOnly
				|| !querySpec.hasSortSpecifications() && querySpec.getSelectClause().isDistinct() ) {
			return new PaginationRenderingPlan.Window( !rowsOnly );
		}
		return new PaginationRenderingPlan.OffsetFetch( true );
	}
}

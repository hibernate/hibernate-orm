/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Standard immutable fixed-plan pagination profiles.
///
/// Conditional dialect families may implement [PaginationRenderingSupport]
/// directly and reuse the immutable plans exposed by these profiles.
///
/// @since 8.0
/// @author Steve Ebersole
public enum StandardPaginationRenderingSupport implements PaginationRenderingSupport {
	OFFSET_FETCH(new PaginationRenderingPlan.OffsetFetch( true )),
	OFFSET_FETCH_WITHOUT_ROWS_KEYWORD(new PaginationRenderingPlan.OffsetFetch( false )),
	LIMIT_OFFSET(new PaginationRenderingPlan.LimitOffset()),
	COMBINED_LIMIT(new PaginationRenderingPlan.CombinedLimit()),
	TOP(new PaginationRenderingPlan.Top( false, true )),
	TOP_WITH_OFFSET(new PaginationRenderingPlan.Top( true, true )),
	TOP_WITH_OFFSET_WITHOUT_PARENTHESES(new PaginationRenderingPlan.Top( true, false )),
	TOP_START_AT(new PaginationRenderingPlan.TopStartAt()),
	FIRST_SKIP(new PaginationRenderingPlan.FirstSkip()),
	SKIP_FIRST(new PaginationRenderingPlan.SkipFirst()),
	FIRST(new PaginationRenderingPlan.First()),
	ROWS_TO(new PaginationRenderingPlan.RowsTo()),
	FETCH_PLUS_OFFSET(new PaginationRenderingPlan.FetchPlusOffset()),
	WINDOW(new PaginationRenderingPlan.Window( true )),
	NONE(new PaginationRenderingPlan.None());

	private final PaginationRenderingPlan plan;

	StandardPaginationRenderingSupport(PaginationRenderingPlan plan) {
		this.plan = plan;
	}

	@Override
	public PaginationRenderingPlan determinePlan(PaginationRenderingRequest request) {
		return plan;
	}
}

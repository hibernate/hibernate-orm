/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

/// Dialect strategy for selecting the SQL rendering plan for a paginated query
/// part.
///
/// A custom translator should normally return one of the reusable strategies in
/// [StandardPaginationRenderingSupport] from its
/// `getPaginationRenderingSupport()` hook. Implement this contract only when
/// plan selection depends on different semantic conditions. Implementations
/// select behavior only: the translator retains ownership of SQL rendering,
/// traversal stacks, and parameter bindings.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface PaginationRenderingSupport {
	/// Select the rendering plan for the supplied semantic context.
	///
	/// @return a non-null plan which the translator can render
	PaginationRenderingPlan determinePlan(PaginationRenderingRequest request);
}

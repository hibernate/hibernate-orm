/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

/// Dialect strategy for selecting an immutable rendering plan for a table join.
///
/// Custom translators should normally reuse [StandardTableJoinRenderingSupport]
/// and only implement this contract for database-specific contextual rules.
/// Implementations must use only the supplied read-only request and must not
/// retain translator or SQL AST state.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface TableJoinRenderingSupport {
	/// Select a non-null plan which preserves the requested join semantics, or an
	/// [TableJoinRenderingPlan.Unsupported] plan when no safe form exists.
	TableJoinRenderingPlan determinePlan(TableJoinRenderingRequest request);
}

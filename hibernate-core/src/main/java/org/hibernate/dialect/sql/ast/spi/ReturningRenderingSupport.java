/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

/// Dialect strategy for selecting an immutable plan for returning generated
/// column values from a mutation.
///
/// A custom translator should normally reuse a strategy in
/// [StandardReturningRenderingSupport]. Implement this contract only when the
/// choice between a trailing clause and a data-change table depends on
/// different semantic facts. Implementations must not retain translator or SQL
/// AST state.
///
/// @see AbstractSqlAstTranslator#getReturningRenderingSupport()
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface ReturningRenderingSupport {
	/// Select a non-null plan for the requested mutation and returned columns.
	ReturningRenderingPlan determinePlan(ReturningRenderingRequest request);
}

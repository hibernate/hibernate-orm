/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

/// Dialect strategy for selecting an immutable plan for insert-conflict
/// semantics.
///
/// A custom translator should normally reuse
/// [StandardInsertConflictRenderingSupport]. Implement this contract only when
/// the database needs a different choice among native syntax, merge emulation,
/// or constraint-violation handling. Implementations must not retain translator
/// or SQL AST state.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface InsertConflictRenderingSupport {
	/// Select a non-null plan which preserves the requested conflict action.
	InsertConflictRenderingPlan determinePlan(InsertConflictRenderingRequest request);
}

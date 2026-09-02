/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

/// Dialect strategy for selecting an immutable rendering plan for a
/// derived-table reference.
///
/// A custom translator should normally reuse
/// [StandardDerivedTableRenderingSupport] and only implement this contract when
/// its derived-table syntax requires different contextual choices.
/// Implementations must use only the supplied read-only request and must not
/// retain translator or SQL AST state. The translator remains responsible for
/// traversal, aliases, and SQL emission.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface DerivedTableRenderingSupport {
	/// Select a non-null plan compatible with [DerivedTableRenderingRequest#kind].
	DerivedTableRenderingPlan determinePlan(DerivedTableRenderingRequest request);
}

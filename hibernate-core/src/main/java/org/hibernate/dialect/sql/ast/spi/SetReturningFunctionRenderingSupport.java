/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

/// Dialect strategy for selecting an immutable rendering plan for a named
/// set-returning function.
///
/// A custom translator should normally reuse one of the strategies in
/// [StandardSetReturningFunctionRenderingSupport]. Implement this contract only
/// when invocation wrapping or ordinality emulation differs. Implementations
/// must use only the supplied read-only request and must not retain translator
/// or SQL AST state.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(SPI.Role.IMPLEMENT)
public interface SetReturningFunctionRenderingSupport {
	/// Select a non-null native, emulated, or unsupported plan.
	SetReturningFunctionRenderingPlan determinePlan(SetReturningFunctionRenderingRequest request);
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import jakarta.annotation.Nullable;

/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// a named set-returning function.
///
/// Implementations of [SetReturningFunctionRenderingSupport] must treat this as
/// call-scoped input and must not retain it.
///
/// @since 8.0
/// @author Steve Ebersole
public interface SetReturningFunctionRenderingRequest {
	/// Whether the function table requests an ordinality output column.
	boolean ordinalityRequested();

	/// The output-column name for the requested ordinality value, or `null` when
	/// ordinality was not requested.
	@Nullable String ordinalityColumnName();
}

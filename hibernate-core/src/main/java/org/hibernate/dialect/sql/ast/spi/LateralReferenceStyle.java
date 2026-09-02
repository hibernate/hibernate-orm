/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Selects how a lateral derived-table reference is introduced.
///
/// @since 8.0
/// @author Steve Ebersole
public enum LateralReferenceStyle {
	/// Render the standard `lateral` keyword.
	KEYWORD,
	/// Render no introducer because correlation is implicit in this context.
	IMPLICIT,
	/// Emulate correlation by rewriting the derived query part.
	EMULATED_QUERY_PART,
	/// Emulate correlation by unnesting an aggregate array value.
	ARRAY_UNNEST
}

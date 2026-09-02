/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Selects the SQL form used for a values-table source.
///
/// @since 8.0
/// @author Steve Ebersole
public enum ValuesTableRenderingStyle {
	/// Native `values (...), (...)` table syntax.
	VALUES,
	/// A sequence of select statements combined with `union all`.
	SELECT_UNION
}

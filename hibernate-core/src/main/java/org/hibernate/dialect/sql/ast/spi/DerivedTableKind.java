/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The semantic kind of derived table being rendered.
///
/// @since 8.0
/// @author Steve Ebersole
public enum DerivedTableKind {
	/// A parenthesized query specification or query group.
	QUERY_PART,
	/// A query part inlined to emulate a correlated common-table expression.
	INLINE_CTE,
	/// A table constructed from row-value expressions.
	VALUES,
	/// A function used as a table source.
	FUNCTION
}

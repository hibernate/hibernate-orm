/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Read-only semantic context for rendering a prefix before a primary table
/// reference.
///
/// Hibernate supplies this context to
/// [AbstractSqlAstTranslator#renderPrimaryTableReferencePrefix]. Overrides
/// should inspect it only to choose a prefix and must not retain it.
///
/// @since 8.0
/// @author Steve Ebersole
public interface PrimaryTableReferenceContext {
	/// The structural kind of primary table reference.
	PrimaryTableReferenceKind kind();

	/// Whether the reference is a query-like construct requiring subquery
	/// placement rules.
	boolean subqueryLike();

	/// Whether this reference begins a parenthesized nested-join group.
	boolean beginsNestedJoinGroup();
}

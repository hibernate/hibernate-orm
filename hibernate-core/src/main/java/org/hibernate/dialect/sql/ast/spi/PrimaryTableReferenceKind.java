/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The semantic kind of primary table reference.
///
/// Hibernate supplies this classification to
/// [AbstractSqlAstTranslator#renderPrimaryTableReferencePrefix] before it
/// renders the primary reference of a table group. It lets a translator choose
/// an introducer without inspecting internal SQL AST implementations.
///
/// @since 8.0
/// @author Steve Ebersole
public enum PrimaryTableReferenceKind {
	/// A physical named table or view, for example `book b`.
	NAMED,
	/// A derived query part, for example `(select ... from ...) q`.
	QUERY_PART,
	/// A derived values table, for example `(values (1), (2)) v(id)`.
	VALUES,
	/// A function used as a table source, for example `unnest(tags) u(tag)`.
	FUNCTION
}

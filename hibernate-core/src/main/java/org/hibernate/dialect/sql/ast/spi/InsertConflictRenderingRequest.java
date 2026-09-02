/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// an insert-conflict clause.
///
/// Implementations of [InsertConflictRenderingSupport] must not mutate the
/// predicate or retain this request after plan selection.
///
/// @since 8.0
/// @author Steve Ebersole
public interface InsertConflictRenderingRequest {
	/// The conflict action requested by the SQL AST.
	InsertConflictAction action();

	/// The explicitly named constraint, or `null` when none was named.
	String constraintName();

	/// Whether the conflict target contains one or more constraint columns.
	boolean hasConstraintColumns();

	/// The conflict-target predicate, or an empty predicate when none is present.
	Predicate predicate();
}

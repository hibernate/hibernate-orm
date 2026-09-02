/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Semantic action requested by an insert conflict clause.
///
/// @since 8.0
/// @author Steve Ebersole
public enum InsertConflictAction {
	/// No conflict clause was requested.
	NONE,
	/// Ignore the proposed row when it conflicts.
	DO_NOTHING,
	/// Update the existing row when it conflicts.
	DO_UPDATE
}

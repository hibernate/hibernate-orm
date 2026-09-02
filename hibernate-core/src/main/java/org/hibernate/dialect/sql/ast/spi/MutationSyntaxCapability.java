/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// A native SQL mutation-syntax capability.
///
/// The mutation kind is supplied separately when the capability is queried.
///
/// @since 8.0
/// @author Steve Ebersole
public enum MutationSyntaxCapability {
	/// A `FROM` clause is supported by the mutation statement.
	FROM_CLAUSE,
	/// Joins are supported by the mutation statement.
	JOIN,
	/// The mutation statement must contain a `WHERE` clause.
	REQUIRES_WHERE
}

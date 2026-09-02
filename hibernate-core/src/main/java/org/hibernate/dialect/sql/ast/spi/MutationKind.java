/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The kind of SQL mutation statement whose syntax is being described.
///
/// @since 8.0
/// @author Steve Ebersole
public enum MutationKind {
	/// An SQL `INSERT` statement.
	INSERT,
	/// An SQL `UPDATE` statement.
	UPDATE,
	/// An SQL `DELETE` statement.
	DELETE
}

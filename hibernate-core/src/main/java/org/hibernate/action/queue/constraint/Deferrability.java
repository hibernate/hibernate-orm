/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

/// Style of deferrability for database constraints.
///
/// @author Steve Ebersole
public enum Deferrability {
	/// Deferrability is not known / unspecified.
	///
	/// @see #NOT_DEFERRABLE
	UNKNOWN,

	/// The constraint check cannot be deferred; it is checked after every statement.
	///
	/// @see java.sql.DatabaseMetaData#importedKeyNotDeferrable
	NOT_DEFERRABLE,

	/// The constraint is checked after each statement by default but can be deferred.
	///
	/// @see java.sql.DatabaseMetaData#importedKeyInitiallyImmediate
	INITIALLY_IMMEDIATE,

	/// The constraint check is deferred until the end of the transaction by default.
	///
	/// @see java.sql.DatabaseMetaData#importedKeyInitiallyDeferred
	INITIALLY_DEFERRED;

	/// Corresponding [java.sql.DatabaseMetaData] constant value.
	public int getCorrespondingJdbcConstant() {
		return switch ( this ) {
			case UNKNOWN, NOT_DEFERRABLE -> 7;
			case INITIALLY_IMMEDIATE -> 6;
			case INITIALLY_DEFERRED -> 5;
		};
	}

	public String getCorrespondingSqlString() {
		return switch ( this ) {
			case UNKNOWN, NOT_DEFERRABLE -> "NOT DEFERRABLE";
			case INITIALLY_IMMEDIATE -> "DEFERRABLE INITIALLY IMMEDIATE";
			case INITIALLY_DEFERRED -> "DEFERRABLE INITIALLY DEFERRED";
		};
	}

	public boolean isDeferrable() {
		return this == INITIALLY_IMMEDIATE || this == INITIALLY_DEFERRED;
	}
}

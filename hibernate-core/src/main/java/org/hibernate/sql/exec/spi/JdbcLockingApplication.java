/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.SPI;

/// Records which stage owns pessimistic locking for a JDBC select plan.
///
/// Exactly one stage must own a requested lock. Execution finalization uses
/// this value to avoid applying raw-SQL or follow-on locking to a plan whose SQL
/// AST translator already rendered locking syntax.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public enum JdbcLockingApplication {
	/// The select does not request pessimistic locking.
	NONE,
	/// Locking syntax was rendered while translating the SQL AST.
	RENDERED,
	/// Locking must still be attempted against the completed SQL string before
	/// execution.
	RAW_SQL,
	/// Locking is performed by follow-on actions after the primary select.
	FOLLOW_ON
}

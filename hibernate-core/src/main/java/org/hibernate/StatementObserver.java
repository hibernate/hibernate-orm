/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/// Observation of (almost) all JDBC [statements][java.sql.Statement] performed by Hibernate.
///
/// Generally "performed" means calls to [java.sql.Statement#execute], [java.sql.Statement#executeQuery]
/// or [java.sql.Statement#executeUpdate].  In these cases, [#performingSql] is called with `-1` as the
/// `batchPosition`.
///
/// In JDBC batching cases, [#performingSql] is called for each [java.sql.Statement#addBatch] call.  In these
/// cases, `batchPosition` is the addition's position within the current batch.
///
/// @apiNote Also provides [#swallowSql(String, int)] as a simple npo-op reference.
///
/// @since 8.0
///
/// @author Steve Ebersole
@Incubating
public interface StatementObserver {
	/// Callback that the given `sql` is about to be performed.
	///
	/// @apiNote "Performed" here could mean immediately executed, or added to a JDBC batch.
	///
	/// @param sql The SQL which is being performed.
	/// @param batchPosition The position within a batch; `-1` if not batched.
	void performingSql(String sql, int batchPosition);

	/// Simple "black hole" for "no observer".
	static void swallowSql(String sql, int batchPosition) {
	}
}

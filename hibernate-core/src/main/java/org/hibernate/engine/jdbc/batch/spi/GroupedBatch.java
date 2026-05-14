/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;

/// Batch variant based on a group of prepared statements.
///
/// A grouped batch is the batching model used by the general mutation executor
/// infrastructure.  Each logical row may contribute bindings to more than one
/// JDBC statement in the [statement group][#getStatementGroup()], for example
/// when a mutation spans multiple tables.  The supplied [JdbcValueBindings]
/// describe the values for the current logical row, and the batch applies those
/// values to each included statement before calling `PreparedStatement.addBatch()`.
///
/// This contract is intentionally separate from [SingleStatementBatch].  Code
/// that already has a single prepared statement shape and can bind directly to
/// that statement does not need to manufacture a statement group just to use the
/// batching lifecycle.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface GroupedBatch extends Batch {
	/// The group of prepared statements managed by this batch.
	///
	/// Callers may need access to the group when using mutation executors that
	/// expose statement details for generated values handling or other execution
	/// infrastructure.  The returned group is owned by the batch and is released
	/// when the batch is released.
	PreparedStatementGroup getStatementGroup();

	/// Add one logical row to this grouped batch.
	///
	/// The batch applies `jdbcValueBindings` to the statements selected by
	/// `inclusionChecker`.  Passing `null` for `inclusionChecker` means all
	/// statements in the group are included.  If adding the row fills the configured
	/// batch size, the implementation may execute the JDBC batch immediately.
	///
	/// @param jdbcValueBindings values for the current logical mutation row
	/// @param inclusionChecker optional selector for statements/tables that should
	/// be included for this row
	void addToBatch(JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker);

	/// Add one logical row to this grouped batch with stale-state exception mapping.
	///
	/// The mapper is associated with the row being added and is used only if JDBC
	/// row-count verification reports a stale-state condition for that row.  This
	/// lets higher-level mutation code translate or suppress stale-state failures
	/// without changing normal successful batch result processing.
	///
	/// @param jdbcValueBindings values for the current logical mutation row
	/// @param inclusionChecker optional selector for statements/tables that should
	/// be included for this row
	/// @param staleStateMapper optional mapper for stale-state failures associated
	/// with this row
	void addToBatch(
			JdbcValueBindings jdbcValueBindings,
			TableInclusionChecker inclusionChecker,
			StaleStateMapper staleStateMapper);
}

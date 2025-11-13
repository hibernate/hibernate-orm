/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.Incubating;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;

import java.util.Collection;

/// Strategy for dealing with locking via a SQL `FOR UPDATE (OF)`
/// clause.
///
/// Some dialects do not use a `FOR UPDATE (OF)` to apply
/// locks - e.g., they apply locks in the `FROM` clause.  Such
/// dialects would return a no-op version of this contract.
///
/// Some dialects support an additional `FOR SHARE (OF)` clause
/// as well to acquire non-exclusive locks.  That is also handled here,
/// varied by the requested {@linkplain org.hibernate.LockMode LockMode}.
///
/// Operates in 2 "phases"-
/// * collect tables which are to be locked (based on {@linkplain org.hibernate.Locking.Scope}, and other things)
/// * render the appropriate locking fragment
///
/// @implSpec Note that this is also used to determine and track which
/// tables to lock even for cases (T-SQL e.g.) where a "locking clause"
/// per-se won't be used.  In such cases, only the first phase (along
/// with [#shouldLockRoot] and [#shouldLockJoin]) have any impact.
///
/// @see org.hibernate.dialect.Dialect#getLockingClauseStrategy
/// @see org.hibernate.sql.exec.spi.JdbcSelectWithActionsBuilder
///
/// @author Steve Ebersole
@Incubating
public interface LockingClauseStrategy {
	/// Register the given [root][TableGroup]
	/// @return Whether the [root][TableGroup] ought to be locked
	boolean registerRoot(TableGroup root);

	/// Register the given [join][TableGroupJoin]
	/// @return Whether the [join][TableGroupJoin] ought to be locked
	boolean registerJoin(TableGroupJoin join);

	/// Are any outer joins encountered during registration
	/// of [roots][#registerRoot] and [joins][#registerJoin]
	boolean containsOuterJoins();

	/// For cases where a locking clause is to be used,
	/// render that locking clause.
	void render(SqlAppender sqlAppender);

	// All [NavigablePath]s to be locked.
	Collection<NavigablePath> getPathsToLock();
}

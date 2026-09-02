/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;


import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.spi.SqlAppender;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

import java.util.Collection;

/// Per-translation strategy for collecting lock targets and rendering
/// statement-level pessimistic locking.
///
/// Some dialects do not use a `FOR UPDATE (OF)` to apply
/// locks - e.g., they apply locks in the `FROM` clause.  Such
/// dialects would return a no-op version of this contract.
///
/// Some dialects support an additional `FOR SHARE (OF)` clause
/// as well to acquire non-exclusive locks.  That is also handled here,
/// varied by the requested {@linkplain org.hibernate.LockMode LockMode}.
///
/// The translator uses each instance in two ordered phases:
///
/// 1. register roots and joins while traversing the from clause; and
/// 2. render the locking fragment after statement rendering reaches the
///    database-specific clause position.
///
/// Implementations may retain state collected during the first phase, so an
/// instance belongs to one translation and must not be cached or reused. Prefer
/// composing [LockingClauseRenderer] through the standard strategy unless
/// target collection itself differs.
///
/// @implSpec Note that this is also used to determine and track which
/// tables to lock even for cases (T-SQL e.g.) where a "locking clause"
/// per-se won't be used.  In such cases, only the first phase (along
/// with `shouldLockRoot()` and `shouldLockJoin()`) have any impact.
///
/// @see org.hibernate.dialect.Dialect#getLockingClauseStrategy
/// @see org.hibernate.dialect.Dialect#buildLockingClauseStrategy
/// @see org.hibernate.sql.exec.spi.JdbcSelectWithActionsBuilder
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface LockingClauseStrategy {
	/// Register the given root table group during from-clause traversal.
	///
	/// @return whether the translator should render this root as locked
	boolean registerRoot(TableGroup root);

	/// Register the given table-group join during from-clause traversal.
	///
	/// @return whether the translator should render this join as locked
	boolean registerJoin(TableGroupJoin join);

	/// Whether registration encountered an outer join.
	boolean containsOuterJoins();

	/// Whether registration encountered any join.
	boolean containsJoins();

	/// Render the locking fragment at the position selected by the translator.
	/// A no-clause strategy performs no output.
	void render(SqlAppender sqlAppender);

	/// The navigable paths selected for locking during registration.
	Collection<NavigablePath> getPathsToLock();
}

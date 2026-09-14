/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;


import jakarta.persistence.Timeout;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.SPI;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.TableLockHintRendererSupport;

import java.util.List;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Cohesive Dialect profile for pessimistic-locking capabilities and operations.
///
/// A provider supplies one profile from
/// [org.hibernate.dialect.Dialect#getLockingSupport] or a supported
/// family-specific supply point such as
/// [org.hibernate.dialect.AbstractSybaseDialect#getLockingSupport]. Its
/// metadata and strategies must describe the same behavior: for example, a
/// `TABLE_HINT` style must provide a table-hint renderer, and an
/// already-rendered SQL rewriter must return `UNSUPPORTED` when the available
/// structure cannot place those hints safely.
///
/// Hibernate may retain and reuse the profile and its returned components for
/// the lifetime of the Dialect. Implementations should therefore be immutable
/// or thread-safe and must not retain request-specific state. Per-translation
/// target collection belongs to [LockingClauseStrategy], not this profile.
///
/// @since 8.0
/// @author Steve Ebersole
/// @see org.hibernate.dialect.Dialect#getLockingSupport()
/// @see org.hibernate.dialect.AbstractSybaseDialect#getLockingSupport()
@Incubating(since = "7.1", group = "dialect-locking")
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface LockingSupport {
	/// The renderer for a complete statement-level locking clause. Return
	/// [LockingClauseRenderer#NO_OP] when this profile does not use one.
	///
	/// @since 8.0
	/// @see LockingClauseRenderer
	@SPI(SUPPLY)
	LockingClauseRenderer getLockingClauseRenderer();

	/// The renderer for table-level locking hints. The default reports that this
	/// profile does not use table hints.
	///
	/// @since 8.0
	/// @see TableLockHintRenderer
	@SPI(SUPPLY)
	default TableLockHintRenderer getTableLockHintRenderer() {
		return TableLockHintRenderer.NONE;
	}

	/// The strategy for applying locking to already-rendered native or legacy SQL.
	/// The default derives safe placement from this profile's locking style.
	///
	/// @since 8.0
	/// @see LockingSqlRewriter
	@SPI(SUPPLY)
	default LockingSqlRewriter getLockingSqlRewriter() {
		return StandardLockingSqlRewriters.forSupport( this );
	}

	/// The policy for deciding whether completed SQL requires follow-on locking.
	/// The default never requests it.
	///
	/// @since 8.0
	/// @see FollowOnLockingPolicy
	@SPI(SUPPLY)
	default FollowOnLockingPolicy getFollowOnLockingPolicy() {
		return FollowOnLockingPolicy.NEVER;
	}

	/// Renders the table hint, if any, which makes a simple single-table
	/// `select` a *current read*: a read which returns the latest committed
	/// state of the selected rows, waiting for the outcome of any concurrent
	/// uncommitted write to them, instead of returning state from a snapshot.
	/// Hibernate performs a current read for the version check it executes
	/// just before commit for an entity locked in [LockMode#OPTIMISTIC] mode.
	///
	/// When plain reads [already wait][Metadata#readsWaitForUncommittedWrites]
	/// nothing is rendered, since a plain read is already a current read.
	/// Otherwise, by default, a [PessimisticLockStyle#TABLE_HINT] profile
	/// renders its share-lock hint, and other profiles render nothing. A
	/// profile may override this to render a cheaper form which waits for
	/// the outcome of concurrent writes but retains no lock.
	///
	/// @param tableExpression The table reference the hint will follow
	///
	/// @since 8.0
	/// @see #renderCurrentReadClause()
	/// @see Metadata#readsWaitForUncommittedWrites()
	@SPI(SUPPLY)
	default String renderCurrentReadTableHint(String tableExpression) {
		final Metadata metadata = getMetadata();
		return !metadata.readsWaitForUncommittedWrites()
			&& metadata.getPessimisticLockStyle() == PessimisticLockStyle.TABLE_HINT
				? TableLockHintRendererSupport.renderHint(
						this, LockMode.PESSIMISTIC_READ, Timeouts.WAIT_FOREVER, tableExpression )
				: "";
	}

	/// Renders the clause, if any, which, appended to a simple single-table
	/// `select`, makes it a current read, as described for
	/// [#renderCurrentReadTableHint].
	///
	/// When plain reads [already wait][Metadata#readsWaitForUncommittedWrites]
	/// nothing is rendered. Otherwise, by default, a [PessimisticLockStyle#CLAUSE]
	/// profile renders its share-lock clause, and other profiles render nothing.
	/// A profile may override this to render a cheaper form which waits for
	/// the outcome of concurrent writes but retains no lock.
	///
	/// @since 8.0
	/// @see #renderCurrentReadTableHint(String)
	/// @see Metadata#readsWaitForUncommittedWrites()
	@SPI(SUPPLY)
	default String renderCurrentReadClause() {
		final Metadata metadata = getMetadata();
		return !metadata.readsWaitForUncommittedWrites()
			&& metadata.getPessimisticLockStyle() == PessimisticLockStyle.CLAUSE
				? getLockingClauseRenderer().render(
						new LockingClauseRequest( PessimisticLockKind.SHARE, Timeouts.WAIT_FOREVER, List.of() ) )
				: "";
	}

	/// Immutable capability metadata consistent with this profile's renderers and
	/// strategies.
	///
	/// @see Metadata
	@SPI(SUPPLY)
	Metadata getMetadata();

	/// The strategy for applying lock timeouts through the JDBC connection,
	/// generally by executing a database command. Return
	/// [ConnectionLockTimeoutStrategy#NONE] when unsupported.
	///
	/// @see ConnectionLockTimeoutStrategy
	@SPI(SUPPLY)
	ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy();

	/// Immutable description of the locking syntax and timeout capabilities of a
	/// Dialect.
	///
	/// @since 8.0
	/// @see #getMetadata()
	@SPI({ USE, IMPLEMENT, SUPPLY })
	interface Metadata {
		/// The [PessimisticLockStyle] supported by this Dialect.
		///
		/// @see #getLockTimeoutType(Timeout)
		default PessimisticLockStyle getPessimisticLockStyle() {
			return PessimisticLockStyle.CLAUSE;
		}

		/// Reports how this Dialect supports the given timeout value.
		///
		/// @see #getPessimisticLockStyle()
		default LockTimeoutType getLockTimeoutType(Timeout timeout) {
			// matches legacy definition from Dialect
			return switch ( timeout.milliseconds() ) {
				case Timeouts.NO_WAIT_MILLI, Timeouts.SKIP_LOCKED_MILLI -> LockTimeoutType.NONE;
				case Timeouts.WAIT_FOREVER_MILLI -> LockTimeoutType.QUERY;
				default -> LockTimeoutType.NONE;
			};
		}

		/// The [RowLockStrategy] for indicating which rows to lock as part of a
		/// `for share of` style clause. By default, this uses
		/// [#getWriteRowLockStrategy()].
		default RowLockStrategy getReadRowLockStrategy() {
			return getWriteRowLockStrategy();
		}

		/// The [RowLockStrategy] for indicating which rows to lock as part of a
		/// `for update of` style clause.
		default RowLockStrategy getWriteRowLockStrategy() {
			// by default, we report no support
			return RowLockStrategy.NONE;
		}

		/// Whether an ordinary, non-locking `select` waits for the outcome of any
		/// uncommitted write to the rows it reads, as it does on a database with
		/// lock-based readers. A database with multiversion concurrency control,
		/// or with "currently committed" semantics, instead returns the last
		/// committed state of the row without waiting, and must report `false`.
		///
		/// Hibernate uses this to decide whether the version check it performs
		/// just before commit for an entity locked in
		/// [org.hibernate.LockMode#OPTIMISTIC] mode must be rendered as a
		/// [current read][LockingSupport#renderCurrentReadClause], so that the
		/// check waits for a concurrent writer of the row and sees the current
		/// version rather than a snapshot. When plain reads already wait, the
		/// check is a plain read.
		///
		/// Report `false` whenever the behavior depends on database configuration
		/// which the Dialect cannot detect, for example `READ_COMMITTED_SNAPSHOT`
		/// on SQL Server or `CUR_COMMIT` on DB2 LUW.
		///
		/// @since 8.0
		/// @see LockingSupport#renderCurrentReadTableHint(String)
		/// @see LockingSupport#renderCurrentReadClause()
		default boolean readsWaitForUncommittedWrites() {
			return true;
		}

		/// The type of support for outer joins with pessimistic locking.
		OuterJoinLockingType getOuterJoinLockingType();

		/// Whether the Dialect supports supplying a specific lock-timeout wait
		/// period through query options, for example `for update (of)`.
		///
		/// @see #getPessimisticLockStyle
		/// @see PessimisticLockStyle#CLAUSE
		/// @see PessimisticLockStyle#TABLE_HINT
		///
		/// @deprecated Use [#getPessimisticLockStyle] with a
		/// [Timeouts#isRealTimeout real timeout value] instead.
		///
		/// @apiNote This exists temporarily while the legacy locking-clause
		/// rendering overloads are migrated.
		@Deprecated
		default boolean supportsWait() {
			// assume (definitely not always valid, but...) that if the Dialect
			// supports no-wait, it also supports wait.
			return supportsNoWait();
		}

		/// Whether the Dialect supports specifying no-wait through query options.
		///
		/// @see #getPessimisticLockStyle
		/// @see PessimisticLockStyle#CLAUSE
		/// @see PessimisticLockStyle#TABLE_HINT
		///
		/// @deprecated Use [#getPessimisticLockStyle] with [Timeouts#NO_WAIT]
		/// instead.
		///
		/// @apiNote This exists temporarily while the legacy locking-clause
		/// rendering overloads are migrated.
		@Deprecated
		default boolean supportsNoWait() {
			return getLockTimeoutType( Timeouts.NO_WAIT ) == LockTimeoutType.QUERY;
		}

		/// Whether the Dialect supports specifying skip-locked through query
		/// options.
		///
		/// @apiNote This exists temporarily while the legacy locking-clause
		/// rendering overloads are migrated.
		/// @see #getPessimisticLockStyle
		/// @see PessimisticLockStyle#CLAUSE
		/// @see PessimisticLockStyle#TABLE_HINT
		/// @deprecated Use [#getPessimisticLockStyle] with
		/// [Timeouts#SKIP_LOCKED] instead.
		@Deprecated
		default boolean supportsSkipLocked() {
			return getLockTimeoutType( Timeouts.SKIP_LOCKED ) == LockTimeoutType.QUERY;
		}
	}
}

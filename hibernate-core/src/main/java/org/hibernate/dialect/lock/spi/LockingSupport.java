/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;


import jakarta.persistence.Timeout;
import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.PessimisticLockStyle;

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
@Incubating
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

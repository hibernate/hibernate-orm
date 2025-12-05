/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import jakarta.persistence.Timeout;
import org.hibernate.Incubating;
import org.hibernate.Timeouts;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;

/**
 * Details and operations related to a Dialect's support for pessimistic locking.
 *
 * @todo (db-locking) : we really need to add distinct support here for read/write locking.
 * 		especially as I move actual building of the lock-strings here.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface LockingSupport {
	/**
	 * Access to details about the locking capabilities of a Dialect.
	 */
	Metadata getMetadata();

	/**
	 * Access to the delegate which can be used for applying lock timeouts
	 * using the {@linkplain java.sql.Connection JDBC connection}, generally
	 * via execution of a command/statement.
	 */
	ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy();

	/**
	 * Describes the locking capabilities of a Dialect.
	 */
	interface Metadata {
		/**
		 * The PessimisticLockStyle supported by this Dialect.
		 *
		 * @see #getLockTimeoutType(Timeout)
		 */
		default PessimisticLockStyle getPessimisticLockStyle() {
			return PessimisticLockStyle.CLAUSE;
		}

		/**
		 * How (if) this Dialect supports the given timeout value.
		 *
		 * @see #getPessimisticLockStyle()
		 */
		default LockTimeoutType getLockTimeoutType(Timeout timeout) {
			// matches legacy definition from Dialect
			return switch ( timeout.milliseconds() ) {
				case Timeouts.NO_WAIT_MILLI, Timeouts.SKIP_LOCKED_MILLI -> LockTimeoutType.NONE;
				case Timeouts.WAIT_FOREVER_MILLI -> LockTimeoutType.QUERY;
				default -> LockTimeoutType.NONE;
			};
		}

		/**
		 * The {@linkplain RowLockStrategy strategy} for indicating which rows
		 * to lock as part of a {@code for share of} style clause.
		 * <p/>
		 * By default, simply uses {@linkplain #getWriteRowLockStrategy()}.
		 */
		default RowLockStrategy getReadRowLockStrategy() {
			return getWriteRowLockStrategy();
		}

		/**
		 * The {@linkplain RowLockStrategy strategy} for indicating which rows
		 * to lock as part of a {@code for update of} style clause.
		 */
		default RowLockStrategy getWriteRowLockStrategy() {
			// by default, we report no support
			return RowLockStrategy.NONE;
		}

		/**
		 * The type of support for outer joins with pessimistic locking.
		 */
		OuterJoinLockingType getOuterJoinLockingType();

		/**
		 * Whether the Dialect supports {@code for update (of)} style syntax.
		 *
		 * @deprecated Use {@linkplain #getPessimisticLockStyle}, passing the
		 * necessary lock timeout instead.
		 *
		 * @apiNote This exists, temporarily, to migrate {@linkplain Dialect#supportsForUpdate()}.
		 */
		@Deprecated
		default boolean supportsForUpdate() {
			final PessimisticLockStyle lockStyle = getPessimisticLockStyle();
			return lockStyle == PessimisticLockStyle.CLAUSE;
		}

		/**
		 * Whether the Dialect supports supplying specific lock timeout wait period
		 * via query options (e.g. {@code for update (of) }
		 *
		 * @see #getPessimisticLockStyle
		 * @see PessimisticLockStyle#CLAUSE
		 * @see PessimisticLockStyle#TABLE_HINT
		 *
		 * @deprecated Use {@linkplain #getPessimisticLockStyle}, with a
		 * {@linkplain Timeouts#isRealTimeout real timeout value} instead.
		 *
		 * @apiNote This exists, temporarily, to migrate {@linkplain Dialect#supportsWait()}.
		 */
		@Deprecated
		default boolean supportsWait() {
			// assume (definitely not always valid, but...) that if the Dialect
			// supports no-wait, it also supports wait.
			return supportsNoWait();
		}

		/**
		 * Whether the Dialect supports specifying no-wait via query options.
		 *
		 * @see #getPessimisticLockStyle
		 * @see PessimisticLockStyle#CLAUSE
		 * @see PessimisticLockStyle#TABLE_HINT
		 *
		 * @deprecated Use {@linkplain #getPessimisticLockStyle}, with {@linkplain Timeouts#NO_WAIT} instead.
		 *
		 * @apiNote This exists, temporarily, to migrate {@linkplain Dialect#supportsNoWait()}.
		 */
		@Deprecated
		default boolean supportsNoWait() {
			return getLockTimeoutType( Timeouts.NO_WAIT ) == LockTimeoutType.QUERY;
		}

		/**
		 * Whether the Dialect supports specifying a skip-locked via query options.
		 *
		 * @apiNote This exists, temporarily, to migrate {@linkplain Dialect#supportsSkipLocked()}.
		 * @see #getPessimisticLockStyle
		 * @see PessimisticLockStyle#CLAUSE
		 * @see PessimisticLockStyle#TABLE_HINT
		 * @deprecated Use {@linkplain #getPessimisticLockStyle}, with {@linkplain Timeouts#SKIP_LOCKED} instead.
		 */
		@Deprecated
		default boolean supportsSkipLocked() {
			return getLockTimeoutType( Timeouts.SKIP_LOCKED ) == LockTimeoutType.QUERY;
		}
	}
}

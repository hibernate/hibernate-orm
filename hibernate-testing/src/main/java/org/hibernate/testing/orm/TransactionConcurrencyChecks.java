/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm;

import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;

import static org.hibernate.dialect.lock.spi.Operation.READ;
import static org.hibernate.dialect.lock.spi.Operation.SHARED_LOCK_READ;
import static org.hibernate.dialect.lock.spi.Operation.UPDATE_LOCK_READ;
import static org.hibernate.dialect.lock.spi.Operation.WRITE;
import static org.hibernate.testing.orm.ConcurrencyCheckResult.MATCH;
import static org.hibernate.testing.orm.ConcurrencyCheckResult.NON_MATCH;
import static org.hibernate.testing.orm.ConcurrencyCheckResult.UNDETERMINED;

/// Factory-configuration prerequisites shared by concurrency tests.
/// Obtain the descriptor from the actual factory's JDBC environment.
/// Blocking checks establish exclusion behavior, not successful execution or
/// commit: serialization conflicts, other locks, and database errors can still
/// cause either transaction to fail. They do not establish dirty-read protection.
///
/// @since 8.0
/// @author Steve Ebersole
public final class TransactionConcurrencyChecks {
	private TransactionConcurrencyChecks() {
	}

	/// Whether an uncommitted write retains no protection excluding an ordinary
	/// read of the same existing row by another transaction. A match establishes
	/// only this blocking prerequisite, not successful execution or commit.
	/// Serialization conflicts, other locks, and database errors may still fail
	/// either transaction. Dirty-read protection must be checked separately.
	public static ConcurrencyCheckResult permitsReadWhileWriteUncommitted(TransactionConcurrency concurrency) {
		if ( !concurrency.supports( WRITE ) || !concurrency.supports( READ ) ) {
			return UNDETERMINED;
		}
		return switch ( concurrency.getBlockingDuration( WRITE, READ ) ) {
			case NONE -> MATCH;
			case STATEMENT, TRANSACTION -> NON_MATCH;
			case UNKNOWN, CONDITIONAL -> UNDETERMINED;
		};
	}

	/// Whether protection from an ordinary read permits another transaction to
	/// write the same existing row after the read statement completes, while the
	/// reading transaction remains open. Both NONE and STATEMENT qualify because
	/// neither retains exclusion after the statement completes.
	///
	/// A match establishes only this blocking prerequisite. It does not guarantee
	/// successful execution or commit of either transaction: serialization
	/// conflicts, other locks, and database errors may still cause failure.
	public static ConcurrencyCheckResult permitsWriteAfterReadStatement(TransactionConcurrency concurrency) {
		if ( !concurrency.supports( READ ) || !concurrency.supports( WRITE ) ) {
			return UNDETERMINED;
		}
		return switch ( concurrency.getBlockingDuration( READ, WRITE ) ) {
			case NONE, STATEMENT -> MATCH;
			case TRANSACTION -> NON_MATCH;
			case UNKNOWN, CONDITIONAL -> UNDETERMINED;
		};
	}

	/// Whether a supported ordinary, shared-lock, or update-lock read establishes
	/// the protection needed for stateless optimistic locking. False means support
	/// is not established; it does not prove the opposite of any read guarantee.
	/// This does not select Hibernate's preferred operation or guarantee success
	/// in the presence of concurrent transactions.
	public static boolean supportsStatelessOptimisticLocking(TransactionConcurrency concurrency) {
		for ( var operation : new Operation[] { READ, SHARED_LOCK_READ, UPDATE_LOCK_READ } ) {
			if ( concurrency.supports( operation ) ) {
				final var guarantees = concurrency.getReadGuarantees( operation );
				if ( guarantees.preventsDirtyReads() && guarantees.preventsConcurrentModification()
						&& guarantees.holdsRowLockUntilTransactionCompletion() ) {
					return true;
				}
			}
		}
		return false;
	}
}

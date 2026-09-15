/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

/// Describes how long protection acquired by one operation on a row prevents
/// a conflicting operation in another transaction from accessing that row.
/// Used by [TransactionConcurrency#getBlockingDuration(Operation, Operation)]
/// to describe database behavior for an ordered pair of operations.
///
/// The duration is in relation to the statement or transaction performing the
/// **preceding operation**. It does not measure how long the concurrent
/// operation actually waits, and does not require either operation to be
/// executing when this information is requested.
///
/// For example, [#TRANSACTION] for a shared-lock read followed by a write
/// means that, once the read acquires its protection, a writer in another
/// transaction cannot modify the row until the reading transaction ends.
/// Reversing the operations asks a separate question: how long a write
/// prevents a shared-lock read in another transaction.
///
/// A conflicting operation might fail immediately with `NOWAIT`, time out,
/// become a deadlock victim, or skip the row with `SKIP LOCKED`. These outcomes
/// do not change the lifetime of the protection. Conversely, a serialization
/// failure does not by itself imply blocking.
///
/// These values describe row protection within the scope of
/// [TransactionConcurrency]. They exclude incidental internal waits and
/// assume no explicit early release of protection or rollback to a savepoint.
///
/// @see TransactionConcurrency#getBlockingDuration(Operation, Operation)
///
/// @since 8.0
/// @author Steve Ebersole
public enum BlockingDuration {
	/// The preceding operation does not establish row protection that excludes
	/// the concurrent operation. This does not guarantee that the concurrent
	/// operation will complete immediately or successfully, or that a read will
	/// observe the latest committed state.
	NONE,

	/// The preceding operation can exclude the concurrent operation while its
	/// statement executes. Protection may be released before that statement
	/// completes; this value provides no guarantee of protection after completion.
	STATEMENT,

	/// Once acquired, protection established by the preceding operation excludes
	/// the concurrent operation until the preceding operation's transaction
	/// commits or rolls back. Protection survives completion of the statement.
	TRANSACTION,

	/// Blocking behavior is known to depend on circumstances that this
	/// descriptor does not capture. No particular duration of protection may
	/// be assumed from this value.
	CONDITIONAL,

	/// Blocking behavior has not been established for this pair of operations.
	/// This expresses missing knowledge, whereas [#CONDITIONAL] expresses a
	/// known dependency on additional circumstances. Neither value establishes
	/// a guarantee of protection or absence of blocking.
	UNKNOWN
}

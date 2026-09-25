package org.hibernate.dialect.lock.spi;

/// Guarantees for existing rows successfully returned by a read. A false
/// answer means that callers must not rely on the guarantee. Changes made
/// by the reading transaction itself are excluded.
///
/// @since 8.0
/// @author Steve Ebersole
public interface ReadGuarantees {
	/// Whether values written by other uncommitted transactions are excluded.
	boolean preventsDirtyReads();

	/// Whether later reads of the same kind preserve values and existence of
	/// previously read rows until transaction completion. A snapshot may provide
	/// this without preventing concurrent modification.
	boolean hasStableRowView();

	/// Whether other transactions are prevented from modifying or deleting
	/// returned rows until transaction completion. Snapshot stability or a
	/// possible later serialization failure is insufficient.
	boolean preventsConcurrentModification();

	/// Whether a row lock is acquired and retained until transaction completion.
	boolean holdsRowLockUntilTransactionCompletion();

	/// Whether a successful read observes current committed row state, waiting
	/// for the outcome of a conflicting uncommitted write rather than returning
	/// an older snapshot. A conflict may instead fail the operation. This does
	/// not promise that the state remains unchanged after the read.
	boolean isCurrentRead();
}

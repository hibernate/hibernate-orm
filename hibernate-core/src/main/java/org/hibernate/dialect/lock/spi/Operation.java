package org.hibernate.dialect.lock.spi;

/// A kind of database operation on an existing row, used to ask a
/// [TransactionConcurrency] descriptor about read guarantees and conflicts
/// between transactions.
///
/// These values identify the operation performed, independently of the
/// Hibernate or Jakarta Persistence lock mode that motivated it. They are
/// not isolation levels or an ordering of lock strength. Two values may
/// have the same behavior in a particular database configuration while
/// remaining distinct operations.
///
/// In particular, [#CURRENT_READ] identifies a strategy chosen for read
/// freshness, while [#SHARED_LOCK_READ] and [#UPDATE_LOCK_READ] identify
/// strategies chosen to acquire a particular kind of row lock. A current
/// read may use one of those locks, but need not retain protection after
/// the read completes. An ordinary [#READ] may itself acquire locks or
/// provide current-read semantics because of the configured isolation.
///
/// Consult [TransactionConcurrency#supports(Operation)] before asking about
/// an operation, and use [TransactionConcurrency#getReadGuarantees(Operation)]
/// and [TransactionConcurrency#getBlockingDuration(Operation, Operation)]
/// for its resolved behavior. The operation name alone does not establish
/// lock duration or compatibility with other operations.
///
/// @since 8.0
/// @author Steve Ebersole
public enum Operation {
	/// An ordinary read using the connection's configured isolation, without
	/// an explicit locking clause or current-read hint.
	///
	/// This may read a snapshot, wait for uncommitted writes, or acquire row
	/// locks, depending on the resolved configuration. "Ordinary" does not
	/// imply that the database performs the read without locks.
	READ,

	/// A read using the dialect's explicit strategy for observing current
	/// committed row state. A conflicting uncommitted write must be resolved
	/// before the read succeeds, rather than bypassed by returning an older
	/// snapshot; the conflict may instead cause the read to fail.
	///
	/// The strategy may use a specialized hint, a shared lock, or a lock for
	/// update. Its purpose is freshness at the read, not necessarily protection
	/// against changes after the read. Use the resolved [ReadGuarantees] to
	/// determine whether it also retains a lock or prevents modification until
	/// transaction completion.
	///
	/// An ordinary read that already provides these freshness guarantees is
	/// still described by [#READ]; this value describes the explicit strategy.
	CURRENT_READ,

	/// A read explicitly acquiring an actual shared row lock. Shared locks
	/// permit other shared-lock readers while excluding conflicting writes.
	///
	/// This value describes the database lock actually acquired, not a request
	/// for [org.hibernate.LockMode#PESSIMISTIC_READ]. If a dialect implements
	/// that request by acquiring a lock for update, the resulting operation is
	/// [#UPDATE_LOCK_READ]. Such substitution does not establish support for
	/// this operation.
	SHARED_LOCK_READ,

	/// A read explicitly acquiring the dialect's row lock for subsequent
	/// update. The read reserves the row against competing operations seeking
	/// to update it, but does not itself modify the row.
	///
	/// The database may implement this using an update lock or an exclusive
	/// lock. Compatibility with shared-lock readers and ordinary readers is
	/// database-specific and must be obtained from [TransactionConcurrency].
	/// Unlike [#WRITE], this operation remains a read and has [ReadGuarantees].
	UPDATE_LOCK_READ,

	/// An operation that actually updates or deletes an existing row, rather
	/// than merely locking it for a possible later modification.
	///
	/// Inserts and their effects on predicates or ranges are outside the scope
	/// of this contract. This operation participates in blocking questions but
	/// has no [ReadGuarantees].
	WRITE
}

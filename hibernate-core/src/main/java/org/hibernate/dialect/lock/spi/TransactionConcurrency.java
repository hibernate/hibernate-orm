package org.hibernate.dialect.lock.spi;

/// Immutable, resolved database concurrency rules for a factory's connections.
/// These are hypothetical operations on the same existing row by different
/// transactions using the same configuration, not observations of active
/// sessions. Connection isolation and relevant database settings must remain
/// consistent across the factory.  The described rows belong to transactional tables.
///
/// Predicate and schema locking, phantoms, lock escalation, and incidental
/// internal waits are outside its scope. Snapshot visibility must not be
/// confused with prevention of concurrent modification.
///
/// @since 8.0
/// @author Steve Ebersole
public interface TransactionConcurrency {
	/// Diagnostic identity, not a JDBC isolation constant or ordering of strength.
	String getName();

	/// Whether the actual operation is supported, without substituting another operation.
	boolean supports(Operation operation);

	/// How long protection established by the preceding operation can exclude
	/// the concurrent operation. Direction is significant. UNKNOWN and
	/// CONDITIONAL must not be treated as proof of exclusion or read freshness.
	///
	/// @throws UnsupportedOperationException if either operation is unsupported
	BlockingDuration getBlockingDuration(Operation precedingOperation, Operation concurrentOperation);

	/// Guarantees established by a successfully completed read of this kind.
	///
	/// @throws IllegalArgumentException if the operation is WRITE
	/// @throws UnsupportedOperationException if the operation is unsupported
	ReadGuarantees getReadGuarantees(Operation operation);
}

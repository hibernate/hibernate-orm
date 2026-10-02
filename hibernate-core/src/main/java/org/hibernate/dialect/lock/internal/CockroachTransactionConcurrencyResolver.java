package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// CockroachDB concurrency with conservative serializable lock guarantees.
/// Read-committed locks are durable. Serializable locks may be best-effort
/// unless durable locking has been enabled, which this resolver does not probe.
/// Current reads use FOR UPDATE when shared-lock support is not established.
///
/// See [CockroachDB locking semantics](https://www.cockroachlabs.com/docs/stable/select-for-update).
///
/// @since 8.0
/// @author Steve Ebersole
public final class CockroachTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	private final boolean lockingClause;

	public CockroachTransactionConcurrencyResolver(boolean lockingClause) {
		this.lockingClause = lockingClause;
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == java.sql.Connection.TRANSACTION_NONE
				? BlockingDuration.UNKNOWN
				: BlockingDuration.TRANSACTION;
	}

	@Override
	protected Boolean versionedReads(Integer isolation) {
		return true;
	}

	@Override
	protected boolean supportsExplicitLocks(Integer isolation) {
		return lockingClause && (isolation == null || isolation != TRANSACTION_NONE);
	}

	@Override
	protected boolean supportsSharedLocks(Integer isolation) {
		return isolation != null && isolation == TRANSACTION_READ_COMMITTED;
	}

	@Override
	protected Guarantees lockingReadGuarantees(Integer isolation) {
		final boolean durable = isolation != null && isolation == TRANSACTION_READ_COMMITTED;
		return new Guarantees( true, durable, durable, durable, true );
	}

	@Override
	protected BlockingDuration adjustBlocking(Integer isolation, Operation preceding, Operation concurrent, BlockingDuration duration) {
		if ( (isolation == null || isolation != TRANSACTION_READ_COMMITTED)
				&& (preceding == Operation.UPDATE_LOCK_READ || preceding == Operation.CURRENT_READ)
				&& concurrent != Operation.READ ) {
			return BlockingDuration.CONDITIONAL;
		}
		return duration;
	}
}

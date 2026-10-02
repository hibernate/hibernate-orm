package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// PostgreSQL MVCC reads and explicit shared/update row locks.
///
/// @since 8.0
/// @author Steve Ebersole
public final class PostgreSQLTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final PostgreSQLTransactionConcurrencyResolver INSTANCE = new PostgreSQLTransactionConcurrencyResolver();

	private PostgreSQLTransactionConcurrencyResolver() {
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
		return isolation == null || isolation != TRANSACTION_NONE;
	}

	@Override
	protected Guarantees lockingReadGuarantees(Integer isolation) {
		return new Guarantees( true, true, true, true, true );
	}

	@Override
	protected boolean supportsSharedLocks(Integer isolation) {
		return true;
	}

	@Override
	protected int snapshotIsolation() {
		return TRANSACTION_REPEATABLE_READ;
	}

	@Override
	protected boolean dirtyReads(Integer isolation) {
		return false;
	}
}

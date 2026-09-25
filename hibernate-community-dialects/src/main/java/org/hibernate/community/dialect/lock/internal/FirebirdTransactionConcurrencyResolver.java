package org.hibernate.community.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import org.hibernate.dialect.lock.internal.AbstractTransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// Firebird WITH LOCK reads return the current committed record or fail.
/// Ordinary read-committed visibility remains unknown without transaction options.
///
/// @since 8.0
/// @author Steve Ebersole
public final class FirebirdTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final FirebirdTransactionConcurrencyResolver INSTANCE = new FirebirdTransactionConcurrencyResolver();

	private FirebirdTransactionConcurrencyResolver() {
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == java.sql.Connection.TRANSACTION_NONE
				? BlockingDuration.UNKNOWN
				: BlockingDuration.TRANSACTION;
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
	protected Boolean versionedReads(Integer isolation) {
		// READ COMMITTED depends on RECORD_VERSION / NO RECORD_VERSION.
		return isolation != null && isolation == TRANSACTION_REPEATABLE_READ ? true : null;
	}

	@Override
	protected int snapshotIsolation() {
		return TRANSACTION_REPEATABLE_READ;
	}
}

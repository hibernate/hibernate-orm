package org.hibernate.community.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import java.sql.Connection;
import org.hibernate.dialect.lock.internal.AbstractTransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

/// Altibase current reads and retained FOR UPDATE row protection.
///
/// @since 8.0
/// @author Steve Ebersole
public final class AltibaseTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final AltibaseTransactionConcurrencyResolver INSTANCE = new AltibaseTransactionConcurrencyResolver();

	private AltibaseTransactionConcurrencyResolver() {
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == java.sql.Connection.TRANSACTION_NONE
				? BlockingDuration.UNKNOWN
				: BlockingDuration.TRANSACTION;
	}

	@Override
	protected Boolean versionedReads(Integer isolation) {
		return isolation != null && isolation == Connection.TRANSACTION_READ_COMMITTED ? true : null;
	}

	@Override
	protected boolean supportsExplicitLocks(Integer isolation) {
		return (isolation == null || isolation != Connection.TRANSACTION_NONE);
	}

	@Override
	protected Guarantees lockingReadGuarantees(Integer isolation) {
		return new Guarantees( true, true, true, true, true );
	}
}

package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// H2 MVCC reads, including its native snapshot isolation value.
///
/// @since 8.0
/// @author Steve Ebersole
public final class H2TransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final H2TransactionConcurrencyResolver INSTANCE = new H2TransactionConcurrencyResolver();

	private H2TransactionConcurrencyResolver() {
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
	protected int snapshotIsolation() {
		return 6;
	}

	@Override
	protected boolean knownIsolation(Integer isolation) {
		return super.knownIsolation( isolation ) || isolation != null && isolation == 6;
	}

	@Override
	protected boolean stableReads(Integer isolation) {
		return super.stableReads( isolation ) || isolation != null && isolation == 6;
	}
}

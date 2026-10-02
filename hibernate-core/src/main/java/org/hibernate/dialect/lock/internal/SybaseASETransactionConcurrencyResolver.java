package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// SAP ASE locking reads and the retained shared locks acquired by HOLDLOCK.
/// HOLDLOCK is ignored at READ UNCOMMITTED. The dialect's pessimistic-write
/// hint also acquires a shared lock, so it does not establish UPDATE_LOCK_READ.
///
/// @since 8.0
/// @author Steve Ebersole
public final class SybaseASETransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final SybaseASETransactionConcurrencyResolver INSTANCE = new SybaseASETransactionConcurrencyResolver();

	private SybaseASETransactionConcurrencyResolver() {
	}

	@Override
	protected Boolean versionedReads(Integer isolation) {
		return false;
	}

	@Override
	protected boolean supportsExplicitLocks(Integer isolation) {
		return knownIsolation( isolation ) && isolation != TRANSACTION_READ_UNCOMMITTED;
	}

	@Override
	protected boolean supportsUpdateLocks(Integer isolation) {
		return false;
	}

	@Override
	protected boolean supportsSharedLocks(Integer isolation) {
		return true;
	}

	@Override
	protected Guarantees lockingReadGuarantees(Integer isolation) {
		return new Guarantees( true, true, true, true, true );
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == TRANSACTION_NONE
				? BlockingDuration.UNKNOWN : BlockingDuration.TRANSACTION;
	}
}

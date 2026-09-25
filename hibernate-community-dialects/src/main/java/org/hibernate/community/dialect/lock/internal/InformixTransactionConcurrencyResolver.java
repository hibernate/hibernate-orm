package org.hibernate.community.dialect.lock.internal;

import org.hibernate.dialect.lock.internal.AbstractTransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.Operation;

import static java.sql.Connection.*;

/// Informix current reads with unknown LAST COMMITTED and update-lock retention settings.
///
/// @since 8.0
/// @author Steve Ebersole
public final class InformixTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final InformixTransactionConcurrencyResolver INSTANCE = new InformixTransactionConcurrencyResolver();

	private InformixTransactionConcurrencyResolver() {
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
		// FOR UPDATE provides freshness even with LAST COMMITTED, but cursor
		// locks need not survive cursor closure unless retention is configured.
		return new Guarantees( true, false, false, false, true );
	}

	@Override
	protected BlockingDuration adjustBlocking(Integer isolation, Operation preceding, Operation concurrent, BlockingDuration duration) {
		return (preceding == Operation.CURRENT_READ || preceding == Operation.UPDATE_LOCK_READ)
				&& concurrent != Operation.READ ? BlockingDuration.CONDITIONAL : duration;
	}
}

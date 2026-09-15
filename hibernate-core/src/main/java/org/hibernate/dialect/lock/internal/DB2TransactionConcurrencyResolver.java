/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// Db2 family concurrency, preserving the selected locking strategy.
///
/// @since 8.0
/// @author Steve Ebersole
public final class DB2TransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	private final boolean currentlyCommitted;
	private final boolean sharedLocks;

	public DB2TransactionConcurrencyResolver(boolean currentlyCommitted, boolean sharedLocks) {
		this.currentlyCommitted = currentlyCommitted;
		this.sharedLocks = sharedLocks;
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
		return currentlyCommitted && (isolation == null || isolation == TRANSACTION_READ_COMMITTED) ? null : false;
	}

	@Override
	protected boolean supportsSharedLocks(Integer isolation) {
		return sharedLocks;
	}

	@Override
	protected boolean shortCurrentRead() {
		return currentlyCommitted;
	}
}

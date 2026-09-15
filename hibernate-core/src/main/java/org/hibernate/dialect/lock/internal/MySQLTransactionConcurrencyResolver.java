/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// MySQL and MariaDB concurrency for transactional InnoDB tables.
///
/// @since 8.0
/// @author Steve Ebersole
public final class MySQLTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final MySQLTransactionConcurrencyResolver INSTANCE = new MySQLTransactionConcurrencyResolver();

	private MySQLTransactionConcurrencyResolver() {
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == java.sql.Connection.TRANSACTION_NONE
				? BlockingDuration.UNKNOWN
				: BlockingDuration.TRANSACTION;
	}

	@Override
	protected Boolean versionedReads(Integer isolation) {
		return isolation == null ? null : isolation != TRANSACTION_SERIALIZABLE;
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
}

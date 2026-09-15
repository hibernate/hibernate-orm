/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.BlockingDuration;

import org.hibernate.dialect.lock.internal.AbstractTransactionConcurrencyResolver;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;

import static java.sql.Connection.*;

/// GaussDB versioned reads and explicit shared/update row protection.
///
/// @since 8.0
/// @author Steve Ebersole
public final class GaussDBTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final GaussDBTransactionConcurrencyResolver INSTANCE = new GaussDBTransactionConcurrencyResolver();

	private GaussDBTransactionConcurrencyResolver() {
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
		return true;
	}

	@Override
	protected boolean supportsSharedLocks(Integer isolation) {
		return true;
	}

	@Override
	protected boolean dirtyReads(Integer isolation) {
		return false;
	}
}

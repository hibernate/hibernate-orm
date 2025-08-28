/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.CacheTransactionSynchronization;

/**
 * @author Steve Ebersole
 */
public class NoCachingTransactionSynchronizationImpl implements CacheTransactionSynchronization {
	public static final NoCachingTransactionSynchronizationImpl INSTANCE = new NoCachingTransactionSynchronizationImpl();

	private NoCachingTransactionSynchronizationImpl() {

	}

	@Override
	public long getCachingTimestamp() {
		throw new UnsupportedOperationException("Method not supported when 2LC is not enabled");
	}

	@Override
	public void transactionJoined() {

	}

	@Override
	public void transactionCompleting() {

	}

	@Override
	public void transactionCompleted(boolean successful) {

	}
}

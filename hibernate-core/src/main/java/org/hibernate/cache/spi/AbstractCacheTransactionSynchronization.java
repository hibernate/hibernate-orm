/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCacheTransactionSynchronization implements CacheTransactionSynchronization {
	private long lastTransactionCompletionTimestamp;
	private final RegionFactory regionFactory;

	public AbstractCacheTransactionSynchronization(RegionFactory regionFactory) {
		// prime the timestamp for any non-transactional access - until (if) we
		// 		later join a new txn
		this.lastTransactionCompletionTimestamp = regionFactory.nextTimestamp();
		this.regionFactory = regionFactory;
	}

	@Override
	public long getCachingTimestamp() {
		return lastTransactionCompletionTimestamp;
	}

	@Override
	public final void transactionJoined() {
		// reset the timestamp
		this.lastTransactionCompletionTimestamp = regionFactory.nextTimestamp();
	}

	@Override
	public final void transactionCompleting() {
	}

	@Override
	public void transactionCompleted(boolean successful) {
		// reset the timestamp for any non-transactional access after this
		// 		point - until (if) we later join a new txn
//		this.lastTransactionCompletionTimestamp = regionFactory.nextTimestamp();
	}

}

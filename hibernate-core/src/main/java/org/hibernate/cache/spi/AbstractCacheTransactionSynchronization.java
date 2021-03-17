/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	public long getCurrentTransactionStartTimestamp() {
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

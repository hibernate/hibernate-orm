/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

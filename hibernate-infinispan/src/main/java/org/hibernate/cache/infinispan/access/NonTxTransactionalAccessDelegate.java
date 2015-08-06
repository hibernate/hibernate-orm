/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Delegate for non-transactional caches
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class NonTxTransactionalAccessDelegate extends TransactionalAccessDelegate {
	public NonTxTransactionalAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
		super(region, validator);
	}

	@Override
	@SuppressWarnings("UnusedParameters")
	public boolean insert(SessionImplementor session, Object key, Object value, Object version) throws CacheException {
		if ( !region.checkValid() ) {
			return false;
		}

		// We need to be invalidating even for regular writes; if we were not and the write was followed by eviction
		// (or any other invalidation), naked put that was started after the eviction ended but before this insert
		// ended could insert the stale entry into the cache (since the entry was removed by eviction).
		if ( !putValidator.beginInvalidatingWithPFER(session, key, value)) {
			throw new CacheException(
					"Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName()
			);
		}
		putValidator.setCurrentSession(session);
		try {
			writeCache.remove(key);
		}
		finally {
			putValidator.resetCurrentSession();
		}
		return true;
	}

	@Override
	@SuppressWarnings("UnusedParameters")
	public boolean update(SessionImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		// We update whether or not the region is valid. Other nodes
		// may have already restored the region so they need to
		// be informed of the change.

		// We need to be invalidating even for regular writes; if we were not and the write was followed by eviction
		// (or any other invalidation), naked put that was started after the eviction ended but before this update
		// ended could insert the stale entry into the cache (since the entry was removed by eviction).
		if ( !putValidator.beginInvalidatingWithPFER(session, key, value)) {
			throw new CacheException(
					"Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName()
			);
		}
		putValidator.setCurrentSession(session);
		try {
			writeCache.remove(key);
		}
		finally {
			putValidator.resetCurrentSession();
		}
		return true;
	}

	@Override
	public void unlockItem(SessionImplementor session, Object key) throws CacheException {
		TransactionCoordinator tc = session.getTransactionCoordinator();
		boolean doPFER = tc != null && tc.getTransactionDriverControl().getStatus() == TransactionStatus.COMMITTED;
		if ( !putValidator.endInvalidatingKey(session, key, doPFER) ) {
			// TODO: localization
			log.warn("Failed to end invalidating pending putFromLoad calls for key " + key + " from region "
					+ region.getName() + "; the key won't be cached until invalidation expires.");
		}
	}
}

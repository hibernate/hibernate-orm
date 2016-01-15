/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * Delegate for non-transactional caches
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class NonTxInvalidationCacheAccessDelegate extends InvalidationCacheAccessDelegate {
	public NonTxInvalidationCacheAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
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
			throw log.failedInvalidatePendingPut(key, region.getName());
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
			throw log.failedInvalidatePendingPut(key, region.getName());
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

	protected boolean isCommitted(SessionImplementor session) {
		if (session.isClosed()) {
			// If the session has been closed before transaction ends, so we cannot find out
			// if the transaction was successful and if we can do the PFER.
			// As this can happen only in JTA environment, we can query the TransactionManager
			// directly here.
			TransactionManager tm = region.getTransactionManager();
			if (tm != null) {
				try {
					switch (tm.getStatus()) {
						case Status.STATUS_COMMITTED:
						case Status.STATUS_COMMITTING:
							return true;
						default:
							return false;
					}
				}
				catch (SystemException e) {
					log.debug("Failed to retrieve transaction status", e);
					return false;
				}
			}
		}
		TransactionCoordinator tc = session.getTransactionCoordinator();
		return tc != null && tc.getTransactionDriverControl().getStatus() == TransactionStatus.COMMITTED;
	}

	@Override
	public void unlockItem(SessionImplementor session, Object key) throws CacheException {
		if ( !putValidator.endInvalidatingKey(session, key, isCommitted(session)) ) {
			log.failedEndInvalidating(key, region.getName());
		}
	}

	@Override
	public boolean afterInsert(SessionImplementor session, Object key, Object value, Object version) {
		if ( !putValidator.endInvalidatingKey(session, key, isCommitted(session)) ) {
			log.failedEndInvalidating(key, region.getName());
		}
		return false;
	}

	@Override
	public boolean afterUpdate(SessionImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		if ( !putValidator.endInvalidatingKey(session, key, isCommitted(session)) ) {
			log.failedEndInvalidating(key, region.getName());
		}
		return false;
	}
}

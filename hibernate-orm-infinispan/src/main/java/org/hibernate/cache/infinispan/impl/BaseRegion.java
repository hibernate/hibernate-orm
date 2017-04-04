/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.impl;

import java.util.Collections;
import java.util.Map;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.spi.Region;

import org.hibernate.cache.spi.access.AccessType;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * Support for Infinispan {@link Region}s. Handles common "utility" methods for an underlying named
 * Cache. In other words, this implementation doesn't actually read or write data. Subclasses are
 * expected to provide core cache interaction appropriate to the semantics needed.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseRegion implements Region {

	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( BaseRegion.class );

	protected final String name;
	protected final AdvancedCache cache;
	protected final AdvancedCache localAndSkipLoadCache;
	protected final TransactionManager tm;
	protected final InfinispanRegionFactory factory;

	protected volatile long lastRegionInvalidation = Long.MIN_VALUE;
	protected int invalidations = 0;

	/**
    * Base region constructor.
    *
    * @param cache instance for the region
    * @param name of the region
	 * @param transactionManager transaction manager may be needed even for non-transactional caches.
    * @param factory for this region
    */
	public BaseRegion(AdvancedCache cache, String name, TransactionManager transactionManager, InfinispanRegionFactory factory) {
		this.cache = cache;
		this.name = name;
		this.tm = transactionManager;
		this.factory = factory;
		this.localAndSkipLoadCache = cache.withFlags(
				Flag.CACHE_MODE_LOCAL, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT,
				Flag.SKIP_CACHE_LOAD
		);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getElementCountInMemory() {
		if ( checkValid() ) {
			return localAndSkipLoadCache.size();
		}

		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Not supported; returns -1
	 */
	@Override
	public long getElementCountOnDisk() {
		return -1;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Not supported; returns -1
	 */
	@Override
	public long getSizeInMemory() {
		return -1;
	}

	@Override
	public int getTimeout() {
		// 60 seconds
		return 60000;
	}

	@Override
	public long nextTimestamp() {
		return factory.nextTimestamp();
	}

	@Override
	public Map toMap() {
		if ( checkValid() ) {
			return cache;
		}

		return Collections.EMPTY_MAP;
	}

	@Override
	public void destroy() throws CacheException {
		cache.stop();
	}

	@Override
	public boolean contains(Object key) {
		return checkValid() && cache.containsKey( key );
	}

   /**
    * Checks if the region is valid for operations such as storing new data
    * in the region, or retrieving data from the region.
    *
    * @return true if the region is valid, false otherwise
    */
	public boolean checkValid() {
		return lastRegionInvalidation != Long.MAX_VALUE;
	}

	/**
	 * Tell the TransactionManager to suspend any ongoing transaction.
	 *
	 * @return the transaction that was suspended, or <code>null</code> if
	 *         there wasn't one
	 */
	public Transaction suspend() {
		Transaction tx = null;
		try {
			if ( tm != null ) {
				tx = tm.suspend();
			}
		}
		catch (SystemException se) {
			throw log.cannotSuspendTx(se);
		}
		return tx;
	}

	/**
	 * Tell the TransactionManager to resume the given transaction
	 *
	 * @param tx the transaction to suspend. May be <code>null</code>.
	 */
	public void resume(Transaction tx) {
		try {
			if ( tx != null ) {
				tm.resume( tx );
			}
		}
		catch (Exception e) {
			throw log.cannotResumeTx( e );
		}
	}

	/**
	 * Invalidates the region.
	 */
	public void invalidateRegion() {
		// this is called only from EvictAllCommand, we don't have any ongoing transaction
		beginInvalidation();
		endInvalidation();
	}

	public void beginInvalidation() {
		if (log.isTraceEnabled()) {
			log.trace( "Begin invalidating region: " + name );
		}
		synchronized (this) {
			lastRegionInvalidation = Long.MAX_VALUE;
			++invalidations;
		}
		runInvalidation(getCurrentTransaction() != null);
	}

	public void endInvalidation() {
		synchronized (this) {
			if (--invalidations == 0) {
				lastRegionInvalidation = nextTimestamp();
			}
		}
		if (log.isTraceEnabled()) {
			log.trace( "End invalidating region: " + name );
		}
	}

	public TransactionManager getTransactionManager() {
		return tm;
	}

	// Used to satisfy TransactionalDataRegion.isTransactionAware in subclasses
	@SuppressWarnings("unused")
	public boolean isTransactionAware() {
		return tm != null;
	}

	public AdvancedCache getCache() {
		return cache;
	}

	protected Transaction getCurrentTransaction() {
		try {
			// Transaction manager could be null
			return tm != null ? tm.getTransaction() : null;
		}
		catch (SystemException e) {
			throw log.cannotGetCurrentTx(e);
		}
	}

	protected void checkAccessType(AccessType accessType) {
		if (accessType == AccessType.TRANSACTIONAL && !cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			log.transactionalStrategyNonTransactionalCache();
		}
		else if (accessType == AccessType.READ_WRITE && cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			log.readWriteStrategyTransactionalCache();
		}
	}

	protected void runInvalidation(boolean inTransaction) {
		// If we're running inside a transaction, we need to remove elements one-by-one
		// to clean the context as well (cache.clear() does not do that).
		// When we don't have transaction, we can do a clear operation (since we don't
		// case about context) and can't do the one-by-one remove: remove() on tx cache
		// requires transactional context.
		if ( inTransaction ) {
			log.tracef( "Transaction, clearing one element at the time" );
			Caches.removeAll( localAndSkipLoadCache );
		}
		else {
			log.tracef( "Non-transactional, clear in one go" );
			localAndSkipLoadCache.clear();
		}
	}

	public InfinispanRegionFactory getRegionFactory() {
		return factory;
	}
}

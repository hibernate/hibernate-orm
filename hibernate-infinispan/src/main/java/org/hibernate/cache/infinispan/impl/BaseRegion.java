/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.infinispan.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

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

	private static final Log log = LogFactory.getLog( BaseRegion.class );
	private Transaction currentTransaction;

	private enum InvalidateState {
		INVALID, CLEARING, VALID
	}

	private final String name;
	private final AdvancedCache regionClearCache;
	private final TransactionManager tm;

	private final Object invalidationMutex = new Object();
	private final AtomicReference<InvalidateState> invalidateState =
			new AtomicReference<InvalidateState>( InvalidateState.VALID );
	private volatile Transaction invalidateTransaction;

	private final RegionFactory factory;

	protected final AdvancedCache cache;

   /**
    * Base region constructor.
    *
    * @param cache instance for the region
    * @param name of the region
    * @param factory for this region
    */
	public BaseRegion(AdvancedCache cache, String name, RegionFactory factory) {
		this.cache = cache;
		this.name = name;
		this.tm = cache.getTransactionManager();
		this.factory = factory;
		this.regionClearCache = cache.withFlags(
				Flag.CACHE_MODE_LOCAL, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT
		);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getElementCountInMemory() {
		if ( checkValid() ) {
			return cache.size();
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
		return 600;
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
		try {
			cache.stop();
		}
		finally {
			cache.removeListener( this );
		}
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
		boolean valid = isValid();
		if ( !valid ) {
			synchronized (invalidationMutex) {
				if ( invalidateState.compareAndSet( InvalidateState.INVALID, InvalidateState.CLEARING ) ) {
					try {
						// Even if no transactions are running, a new transaction
						// needs to be started to do clear the region
						// (without forcing autoCommit cache configuration).
						Transaction tx = getCurrentTransaction();
						if ( tx != null ) {
							regionClearCache.clear();
						} else {
							Caches.withinTx( cache, new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									regionClearCache.clear();
									return null;
								}
							} );
						}

						invalidateState.compareAndSet(
								InvalidateState.CLEARING, InvalidateState.VALID
						);
					}
					catch ( Exception e ) {
						if ( log.isTraceEnabled() ) {
							log.trace(
									"Could not invalidate region: "
											+ e.getLocalizedMessage()
							);
						}
					}
				}
			}
			valid = isValid();
		}

		return valid;
	}

	protected boolean isValid() {
		return invalidateState.get() == InvalidateState.VALID;
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
			throw new CacheException( "Could not suspend transaction", se );
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
			throw new CacheException( "Could not resume transaction", e );
		}
	}

   /**
    * Invalidates the region.
    */
   public void invalidateRegion() {
		if (log.isTraceEnabled()) {
			log.trace("Invalidate region: " + name);
		}

		Transaction tx = getCurrentTransaction();
		if ( tx != null ) {
			synchronized ( invalidationMutex ) {
				invalidateTransaction = tx;
				invalidateState.set( InvalidateState.INVALID );
			}
		} else {
			invalidateState.set( InvalidateState.INVALID );
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

	public boolean isRegionInvalidatedInCurrentTx() {
		Transaction tx = getCurrentTransaction();
		return tx != null && tx.equals(invalidateTransaction);
	}

	private Transaction getCurrentTransaction() {
		try {
			// Transaction manager could be null
			return tm != null ? tm.getTransaction() : null;
		} catch (SystemException e) {
			throw new CacheException("Unable to get current transaction", e);
		}
	}

}

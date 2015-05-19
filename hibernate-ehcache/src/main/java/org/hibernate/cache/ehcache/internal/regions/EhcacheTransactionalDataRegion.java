/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.TransactionalDataRegion;

/**
 * An Ehcache specific TransactionalDataRegion.
 * <p/>
 * This is the common superclass entity and collection regions.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class EhcacheTransactionalDataRegion extends EhcacheDataRegion implements TransactionalDataRegion {
	private static final int LOCAL_LOCK_PROVIDER_CONCURRENCY = 128;

	private final SessionFactoryOptions settings;

	/**
	 * Metadata associated with the objects stored in the region.
	 */
	protected final CacheDataDescription metadata;

	private final CacheLockProvider lockProvider;

	/**
	 * Construct an transactional Hibernate cache region around the given Ehcache instance.
	 */
	EhcacheTransactionalDataRegion(
			EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache cache, SessionFactoryOptions settings,
			CacheDataDescription metadata, Properties properties) {
		super( accessStrategyFactory, cache, properties );
		this.settings = settings;
		this.metadata = metadata;

		final Object context = cache.getInternalContext();
		if ( context instanceof CacheLockProvider ) {
			this.lockProvider = (CacheLockProvider) context;
		}
		else {
			this.lockProvider = new StripedReadWriteLockSync( LOCAL_LOCK_PROVIDER_CONCURRENCY );
		}
	}

	/**
	 * Access the Hibernate settings associated with the persistence unit.
	 *
	 * @return settings
	 */
	public SessionFactoryOptions getSettings() {
		return settings;
	}

	@Override
	public boolean isTransactionAware() {
		return false;
	}

	@Override
	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}

	/**
	 * Get the value mapped to this key, or null if no value is mapped to this key.
	 *
	 * @param key The cache key
	 *
	 * @return The cached data
	 */
	public final Object get(Object key) {
		try {
			final Element element = getCache().get( key );
			if ( element == null ) {
				return null;
			}
			else {
				return element.getObjectValue();
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
				return null;
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Map the given value to the given key, replacing any existing mapping for this key
	 *
	 * @param key The cache key
	 * @param value The data to cache
	 *
	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void put(Object key, Object value) throws CacheException {
		try {
			final Element element = new Element( key, value );
			getCache().put( element );
		}
		catch (IllegalArgumentException e) {
			throw new CacheException( e );
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Remove the mapping for this key (if any exists).
	 *
	 * @param key The cache key
	 *
	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void remove(Object key) throws CacheException {
		try {
			getCache().remove( key );
		}
		catch (ClassCastException e) {
			throw new CacheException( e );
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Remove all mapping from this cache region.
	 *
	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void clear() throws CacheException {
		try {
			getCache().removeAll();
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Attempts to write lock the mapping for the given key.
	 *
	 * @param key The cache key
	 *
	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void writeLock(Object key) throws CacheException {
		try {
			lockProvider.getSyncForKey( key ).lock( LockType.WRITE );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Attempts to write unlock the mapping for the given key.
	 *
	 * @param key The cache key
	 *
 	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void writeUnlock(Object key) throws CacheException {
		try {
			lockProvider.getSyncForKey( key ).unlock( LockType.WRITE );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Attempts to read lock the mapping for the given key.
	 *
	 * @param key The cache key
	 *
	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void readLock(Object key) throws CacheException {
		try {
			lockProvider.getSyncForKey( key ).lock( LockType.READ );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Attempts to read unlock the mapping for the given key.
	 *
	 * @param key The cache key
	 *
	 * @throws CacheException Indicates a problem accessing the cache
	 */
	public final void readUnlock(Object key) throws CacheException {
		try {
			lockProvider.getSyncForKey( key ).unlock( LockType.READ );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	/**
	 * Returns <code>true</code> if the locks used by the locking methods of this region are the independent of the cache.
	 * <p/>
	 * Independent locks are not locked by the cache when the cache is accessed directly.  This means that for an independent lock
	 * lock holds taken through a region method will not block direct access to the cache via other means.
	 *
	 * @return true/false.  See discussion above.
	 */
	public final boolean locksAreIndependentOfCache() {
		return lockProvider instanceof StripedReadWriteLockSync;
	}
}

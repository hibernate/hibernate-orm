package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import net.sf.ehcache.Ehcache;

public class TransactionalEhcacheCollectionRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion> implements CollectionRegionAccessStrategy {

	/**
	 * Construct a new collection region access strategy.
	 *
	 * @param region
	 *            the Hibernate region.
	 * @param ehcache
	 *            the cache.
	 * @param settings
	 *            the Hibernate settings.
	 */
	public TransactionalEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, Ehcache ehcache,
			SessionFactoryOptions settings) {
		super(region, settings);
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		try {
			return region().get(key);
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version)
			throws CacheException {
		return null;
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp,
			Object version, boolean minimalPutOverride) throws CacheException {
		try {
			if (minimalPutOverride && region().contains(key)) {
				return false;
			} else if (region().containsInDistributedCache(key)) {
				region().putFromLoad(key, value);
				return true;
			} else {
				region().put(key, value);
				return true;
			}
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}

	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		try {
			region().remove(key);
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		// no-operation
	}

	@Override
	public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateCollectionKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetCollectionId(cacheKey);
	}
}

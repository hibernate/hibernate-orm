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

public class NonStrictReadWriteEhcacheCollectionRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion> implements CollectionRegionAccessStrategy {

	/**
	 * Create a non-strict read/write access strategy accessing the given collection
	 * region.
	 *
	 * @param region
	 *            The wrapped region
	 * @param settings
	 *            The Hibernate settings
	 */
	public NonStrictReadWriteEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region,
			SessionFactoryOptions settings) {
		super(region, settings);
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return region().get(key);
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp,
			Object version, boolean minimalPutOverride) throws CacheException {
		if (minimalPutOverride && region().contains(key)) {
			return false;
		} else if (region().containsInDistributedCache(key)) {
			region().putFromLoad(key, value);
			return true;
		} else {
			region().put(key, value);
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version)
			throws CacheException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		region().remove(key);
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		region().remove(key);
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

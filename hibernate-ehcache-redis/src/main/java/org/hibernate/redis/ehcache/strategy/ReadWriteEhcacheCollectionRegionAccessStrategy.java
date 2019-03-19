package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class ReadWriteEhcacheCollectionRegionAccessStrategy extends
		AbstractReadWriteEhcacheAccessStrategy<EhcacheCollectionRegion> implements CollectionRegionAccessStrategy {

	/**
	 * Create a read/write access strategy accessing the given collection region.
	 *
	 * @param region
	 *            The wrapped region
	 * @param settings
	 *            The Hibernate settings
	 */
	public ReadWriteEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region,
			SessionFactoryOptions settings) {
		super(region, settings);
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
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

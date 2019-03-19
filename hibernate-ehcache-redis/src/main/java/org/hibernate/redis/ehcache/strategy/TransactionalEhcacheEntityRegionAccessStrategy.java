package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import net.sf.ehcache.Ehcache;

public class TransactionalEhcacheEntityRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheEntityRegion>
		implements EntityRegionAccessStrategy {

	/**
	 * Construct a new entity region access strategy.
	 *
	 * @param region
	 *            the Hibernate region.
	 * @param ehcache
	 *            the cache.
	 * @param settings
	 *            the Hibernate settings.
	 */
	public TransactionalEhcacheEntityRegionAccessStrategy(EhcacheEntityRegion region, Ehcache ehcache,
			SessionFactoryOptions settings) {
		super(region, settings);
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value,
			Object currentVersion, Object previousVersion, SoftLock lock) {
		return false;
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
	public EntityRegion getRegion() {
		return region();
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version)
			throws CacheException {
		try {
			region().put(key, value);
			return true;
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
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
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion,
			Object previousVersion) throws CacheException {
		try {
			region().put(key, value);
			return true;
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory,
			String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateEntityKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetEntityId(cacheKey);
	}
}
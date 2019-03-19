package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheNaturalIdRegion;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import net.sf.ehcache.Ehcache;

public class TransactionalEhcacheNaturalIdRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheNaturalIdRegion> implements NaturalIdRegionAccessStrategy {

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
	public TransactionalEhcacheNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion region, Ehcache ehcache,
			SessionFactoryOptions settings) {
		super(region, settings);
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
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
	public NaturalIdRegion getRegion() {
		return region();
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
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
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		try {
			region().put(key, value);
			return true;
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister,
			SharedSessionContractImplementor session) {
		return DefaultCacheKeysFactory.staticCreateNaturalIdKey(naturalIdValues, persister, session);
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetNaturalIdValues(cacheKey);
	}
}

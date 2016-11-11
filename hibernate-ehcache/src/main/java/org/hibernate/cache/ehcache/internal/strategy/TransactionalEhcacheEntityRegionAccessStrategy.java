/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheEntityRegion;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * JTA EntityRegionAccessStrategy.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class TransactionalEhcacheEntityRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheEntityRegion>
		implements EntityRegionAccessStrategy {

	private final Ehcache ehcache;

	/**
	 * Construct a new entity region access strategy.
	 *
	 * @param region the Hibernate region.
	 * @param ehcache the cache.
	 * @param settings the Hibernate settings.
	 */
	public TransactionalEhcacheEntityRegionAccessStrategy(
			EhcacheEntityRegion region,
			Ehcache ehcache,
			SessionFactoryOptions settings) {
		super( region, settings );
		this.ehcache = ehcache;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		return false;
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		try {
			final Element element = ehcache.get( key );
			return element == null ? null : element.getObjectValue();
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public EntityRegion getRegion() {
		return region();
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version)
			throws CacheException {
		//OptimisticCache? versioning?
		try {
			ehcache.put( new Element( key, value ) );
			return true;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return null;
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride) throws CacheException {
		try {
			if ( minimalPutOverride && ehcache.get( key ) != null ) {
				return false;
			}
			//OptimisticCache? versioning?
			ehcache.put( new Element( key, value ) );
			return true;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		try {
			ehcache.remove( key );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		// no-op
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) throws CacheException {
		try {
			ehcache.put( new Element( key, value ) );
			return true;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateEntityKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetEntityId(cacheKey);
	}
}

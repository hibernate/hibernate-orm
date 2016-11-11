/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Ehcache specific read-only collection region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class ReadOnlyEhcacheCollectionRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion>
		implements CollectionRegionAccessStrategy {

	/**
	 * Create a read-only access strategy accessing the given collection region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public ReadOnlyEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return region().get( key );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( minimalPutOverride && region().contains( key ) ) {
			return false;
		}
		else {
			region().put( key, value );
			return true;
		}
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws UnsupportedOperationException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * A no-op since this cache is read-only
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
	}

	@Override
	public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetCollectionId(cacheKey);
	}
}

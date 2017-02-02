/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.cache.jcache.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.JCacheCollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Alex Snaps
 */
public class NonStrictCollectionRegionAccessStrategy
		extends JCacheRegionAccessStrategy<JCacheCollectionRegion>
		implements CollectionRegionAccessStrategy {

	public NonStrictCollectionRegionAccessStrategy(JCacheCollectionRegion jCacheCollectionRegion) {
		super( jCacheCollectionRegion );
	}

	@Override
	public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetCollectionId( cacheKey );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		evict( key );
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import javax.cache.Cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Alex Snaps
 */
public class JCacheGeneralDataRegion extends JCacheRegion implements GeneralDataRegion {

	public JCacheGeneralDataRegion(Cache<Object, Object> cache) {
		super( cache );
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) throws CacheException {
		return cache.get( key );
	}

	@Override
	public void put(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		cache.put( key, value );
	}

	@Override
	public void evict(Object key) throws CacheException {
		cache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		cache.removeAll();
	}

}

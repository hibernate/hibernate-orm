/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.GeneralDataRegion;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
class BaseGeneralDataRegion extends BaseRegion implements GeneralDataRegion {
	private static final Logger LOG = Logger.getLogger( BaseGeneralDataRegion.class.getName() );

	BaseGeneralDataRegion(String name) {
		super( name );
	}

	@Override
	public Object get(Object key) throws CacheException {
		LOG.debugf( "Cache[%s] lookup : key[%s]", getName(), key );
		if ( key == null ) {
			return null;
		}
		Object result = cache.get( key );
		if ( result != null ) {
			LOG.debugf( "Cache[%s] hit: %s", getName(), key );
		}
		return result;
	}

	@Override
	public void put(Object key, Object value) throws CacheException {
		LOG.debugf( "Caching[%s] : [%s] -> [%s]", getName(), key, value );
		if ( key == null || value == null ) {
			LOG.debug( "Key or Value is null" );
			return;
		}
		cache.put( key, value );
	}

	@Override
	public void evict(Object key) throws CacheException {
		LOG.debugf( "Evicting[%s]: %s", getName(), key );
		if ( key == null ) {
			LOG.debug( "Key is null" );
			return;
		}
		cache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		LOG.debugf( "evict cache[%s]", getName() );
		cache.clear();
	}
}

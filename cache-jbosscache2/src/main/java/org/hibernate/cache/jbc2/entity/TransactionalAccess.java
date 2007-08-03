/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.lock.TimeoutException;

import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CacheException;

/**
 * This defines the strategy for transactional access to enity data
 * in JBossCache using its 2.x APIs
 *
 * @author Steve Ebersole
 */
public class TransactionalAccess implements EntityRegionAccessStrategy {
	private static final Logger log = LoggerFactory.getLogger( TransactionalAccess.class );

	private final EntityRegionImpl region;

	public TransactionalAccess(EntityRegionImpl region) {
		this.region = region;
	}

	public EntityRegion getRegion() {
		return region;
	}

	public Object get(Object key, long txTimestamp) throws CacheException {
		try {
			return region.getCacheInstance().get( region.getRegionFqn(), EntityRegionImpl.ITEM );
		}
		catch ( Exception e ) {
			throw new CacheException( e );
		}
	}

	public boolean putFromLoad(
			Object key,
			Object value,
			long txTimestamp,
			Object version) throws CacheException {
		try {
			region.getCacheInstance().putForExternalRead( new Fqn( region.getRegionFqn(), key ), EntityRegionImpl.ITEM, value );
			return true;
		}
		catch ( TimeoutException te) {
			//ignore!
			log.debug( "ignoring write lock acquisition failure" );
			return false;
		}
		catch ( Throwable t ) {
			throw new CacheException( t );
		}
	}

	public boolean putFromLoad(
			Object key,
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride) throws CacheException {
		if ( minimalPutOverride && get( key, txTimestamp ) != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "item already cached: " + key );
			}
			return false;
		}
		return putFromLoad( key, value, txTimestamp, version );
	}

	public SoftLock lockItem(Object key, Object version) throws CacheException {
		return null;
	}

	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	public void unlockItem(Object key, SoftLock lock) throws CacheException {
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
	}

	public boolean insert(Object key, Object value, Object version) throws CacheException {
		try {
			region.getCacheInstance().put( new Fqn( region.getRegionFqn(), key ), EntityRegionImpl.ITEM, value );
		}
		catch ( Throwable t ) {
			throw new CacheException( t );
		}
		return true;
	}

	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		return false;
	}

	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		try {
			region.getCacheInstance().put( new Fqn( region.getRegionFqn(), key ), EntityRegionImpl.ITEM, value );
		}
		catch ( Throwable t ) {
			throw new CacheException( t );
		}
		return true;
	}

	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		return false;
	}

	public void remove(Object key) throws CacheException {
		try {
			region.getCacheInstance().removeNode( new Fqn( region.getRegionFqn(), key ) );
		}
		catch ( Exception e ) {
			throw new CacheException( e );
		}
	}

	public void removeAll() throws CacheException {
		try {
			region.getCacheInstance().removeNode( region.getRegionFqn() );
		}
		catch ( Exception e ) {
			throw new CacheException( e );
		}
	}

	public void evict(Object key) throws CacheException {
		remove( key );
	}

	public void evictAll() throws CacheException {
		removeAll();
	}
}

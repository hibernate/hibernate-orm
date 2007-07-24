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

import org.jboss.cache.Fqn;
import org.jboss.cache.lock.TimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CacheException;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ReadOnlyAccess implements EntityRegionAccessStrategy {
	private static final Log log = LogFactory.getLog( ReadOnlyAccess.class );

	private final EntityRegionImpl region;

	public ReadOnlyAccess(EntityRegionImpl region) {
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
			region.getCacheInstance().putForExternalRead( region.getRegionFqn(), key, value );
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
		return putFromLoad( key, value, txTimestamp, version );
	}

	public SoftLock lockItem(Object key, Object version) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to lock (edit) read only item" );
	}

	public SoftLock lockRegion() throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to lock (edit) read only region" );
	}

	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		log.error( "Illegal attempt to lock (edit) read only item" );
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
		log.error( "Illegal attempt to lock (edit) read only region" );
	}

	public boolean insert(Object key, Object value, Object version) throws CacheException {
		try {
			region.getCacheInstance().put( new Fqn( region.getRegionFqn(), key ), EntityRegionImpl.ITEM, value );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
		return true;
	}

	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		return false;
	}

	public boolean update(
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to lock (edit) read only item" );
	}

	public boolean afterUpdate(
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion,
			SoftLock lock) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to lock (edit) read only item" );
	}

	public void remove(Object key) throws CacheException {
		try {
			region.getCacheInstance().remove( region.getRegionFqn(), key );
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
	}

	public void evictAll() throws CacheException {
	}

	public void destroy() {
		region.destroy();
	}
}

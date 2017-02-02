/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
class ReadOnlyEntityRegionAccessStrategy extends BaseEntityRegionAccessStrategy {
	private static final Logger LOG = Logger.getLogger( ReadOnlyEntityRegionAccessStrategy.class );


	ReadOnlyEntityRegionAccessStrategy(EntityRegionImpl region) {
		super( region );
	}

	/**
	 * This cache is asynchronous hence a no-op
	 */
	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false; //wait until tx complete, see afterInsert().
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		getInternalRegion().put( session, key, value ); //save into cache since the tx is completed
		return true;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		evict( key );
	}

	/**
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		LOG.info( "Illegal attempt to update item cached as read-only : " + key );
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}

	/**
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		LOG.info( "Illegal attempt to update item cached as read-only : " + key );
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}


}

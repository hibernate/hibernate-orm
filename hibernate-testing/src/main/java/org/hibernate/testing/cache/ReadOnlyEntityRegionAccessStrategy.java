/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
class ReadOnlyEntityRegionAccessStrategy extends BaseEntityRegionAccessStrategy {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, ReadOnlyEntityRegionAccessStrategy.class.getName()
	);

	ReadOnlyEntityRegionAccessStrategy(EntityRegionImpl region) {
		super( region );
	}
	/**
	 * This cache is asynchronous hence a no-op
	 */
	@Override
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		return false; //wait until tx complete, see afterInsert().
	}

	@Override
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		getInternalRegion().put( key, value ); //save into cache since the tx is completed
		return true;
	}

	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		evict( key );
	}

	/**
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		LOG.invalidEditOfReadOnlyItem( key );
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}

	/**
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		LOG.invalidEditOfReadOnlyItem( key );
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}


}

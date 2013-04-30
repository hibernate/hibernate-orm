/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.internal.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * Implementation of {@link CollectionRegionAccessStrategy} that handles {@link NonStopCacheException} using
 * {@link HibernateNonstopCacheExceptionHandler}
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class NonstopAwareCollectionRegionAccessStrategy implements CollectionRegionAccessStrategy {
	private final CollectionRegionAccessStrategy actualStrategy;
	private final HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler;

	/**
	 * Constructor accepting the actual {@link CollectionRegionAccessStrategy} and the {@link HibernateNonstopCacheExceptionHandler}
	 *
	 * @param actualStrategy The wrapped strategy
	 * @param hibernateNonstopExceptionHandler The exception handler
	 */
	public NonstopAwareCollectionRegionAccessStrategy(
			CollectionRegionAccessStrategy actualStrategy,
			HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler) {
		this.actualStrategy = actualStrategy;
		this.hibernateNonstopExceptionHandler = hibernateNonstopExceptionHandler;
	}

	@Override
	public CollectionRegion getRegion() {
		return actualStrategy.getRegion();
	}

	@Override
	public void evict(Object key) throws CacheException {
		try {
			actualStrategy.evict( key );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	@Override
	public void evictAll() throws CacheException {
		try {
			actualStrategy.evictAll();
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	@Override
	public Object get(Object key, long txTimestamp) throws CacheException {
		try {
			return actualStrategy.get( key, txTimestamp );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	@Override
	public SoftLock lockItem(Object key, Object version) throws CacheException {
		try {
			return actualStrategy.lockItem( key, version );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	@Override
	public SoftLock lockRegion() throws CacheException {
		try {
			return actualStrategy.lockRegion();
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	@Override
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		try {
			return actualStrategy.putFromLoad( key, value, txTimestamp, version, minimalPutOverride );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
		try {
			return actualStrategy.putFromLoad( key, value, txTimestamp, version );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public void remove(Object key) throws CacheException {
		try {
			actualStrategy.remove( key );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	@Override
	public void removeAll() throws CacheException {
		try {
			actualStrategy.removeAll();
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		try {
			actualStrategy.unlockItem( key, lock );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	@Override
	public void unlockRegion(SoftLock lock) throws CacheException {
		try {
			actualStrategy.unlockRegion( lock );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

}

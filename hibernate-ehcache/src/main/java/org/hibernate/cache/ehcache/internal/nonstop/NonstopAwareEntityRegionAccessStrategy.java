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
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * Implementation of {@link EntityRegionAccessStrategy} that handles {@link net.sf.ehcache.constructs.nonstop.NonStopCacheException} using
 * {@link HibernateNonstopCacheExceptionHandler}
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class NonstopAwareEntityRegionAccessStrategy implements EntityRegionAccessStrategy {

	private final EntityRegionAccessStrategy actualStrategy;
	private final HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler;

	/**
	 * Constructor accepting the actual {@link EntityRegionAccessStrategy} and the {@link HibernateNonstopCacheExceptionHandler}
	 *
	 * @param actualStrategy
	 * @param hibernateNonstopExceptionHandler
	 */
	public NonstopAwareEntityRegionAccessStrategy(EntityRegionAccessStrategy actualStrategy,
												  HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler) {
		this.actualStrategy = actualStrategy;
		this.hibernateNonstopExceptionHandler = hibernateNonstopExceptionHandler;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#getRegion()
	 */
	public EntityRegion getRegion() {
		return actualStrategy.getRegion();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#afterInsert(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		try {
			return actualStrategy.afterInsert( key, value, version );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#afterUpdate(java.lang.Object, java.lang.Object, java.lang.Object,
	 *	  java.lang.Object, org.hibernate.cache.spi.access.SoftLock)
	 */
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		try {
			return actualStrategy.afterUpdate( key, value, currentVersion, previousVersion, lock );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#evict(java.lang.Object)
	 */
	public void evict(Object key) throws CacheException {
		try {
			actualStrategy.evict( key );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#evictAll()
	 */
	public void evictAll() throws CacheException {
		try {
			actualStrategy.evictAll();
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#get(java.lang.Object, long)
	 */
	public Object get(Object key, long txTimestamp) throws CacheException {
		try {
			return actualStrategy.get( key, txTimestamp );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#insert(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		try {
			return actualStrategy.insert( key, value, version );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#lockItem(java.lang.Object, java.lang.Object)
	 */
	public SoftLock lockItem(Object key, Object version) throws CacheException {
		try {
			return actualStrategy.lockItem( key, version );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#lockRegion()
	 */
	public SoftLock lockRegion() throws CacheException {
		try {
			return actualStrategy.lockRegion();
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object,
	 *	  boolean)
	 */
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		try {
			return actualStrategy.putFromLoad( key, value, txTimestamp, version, minimalPutOverride );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)
	 */
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
		try {
			return actualStrategy.putFromLoad( key, value, txTimestamp, version );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#remove(java.lang.Object)
	 */
	public void remove(Object key) throws CacheException {
		try {
			actualStrategy.remove( key );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#removeAll()
	 */
	public void removeAll() throws CacheException {
		try {
			actualStrategy.removeAll();
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#unlockItem(java.lang.Object, org.hibernate.cache.spi.access.SoftLock)
	 */
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		try {
			actualStrategy.unlockItem( key, lock );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#unlockRegion(org.hibernate.cache.spi.access.SoftLock)
	 */
	public void unlockRegion(SoftLock lock) throws CacheException {
		try {
			actualStrategy.unlockRegion( lock );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#update(java.lang.Object, java.lang.Object, java.lang.Object,
	 *	  java.lang.Object)
	 */
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		try {
			return actualStrategy.update( key, value, currentVersion, previousVersion );
		}
		catch ( NonStopCacheException nonStopCacheException ) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}
}
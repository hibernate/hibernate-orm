/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.nonstop;

import java.io.Serializable;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.EntityCacheKey;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

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
	 * @param actualStrategy The wrapped EntityRegionAccessStrategy
	 * @param hibernateNonstopExceptionHandler The exception handler
	 */
	public NonstopAwareEntityRegionAccessStrategy(
			EntityRegionAccessStrategy actualStrategy,
			HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler) {
		this.actualStrategy = actualStrategy;
		this.hibernateNonstopExceptionHandler = hibernateNonstopExceptionHandler;
	}

	@Override
	public EntityRegion getRegion() {
		return actualStrategy.getRegion();
	}

	@Override
	public boolean afterInsert(EntityCacheKey key, Object value, Object version) throws CacheException {
		try {
			return actualStrategy.afterInsert( key, value, version );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public boolean afterUpdate(EntityCacheKey key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		try {
			return actualStrategy.afterUpdate( key, value, currentVersion, previousVersion, lock );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public void evict(EntityCacheKey key) throws CacheException {
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
	public Object get(EntityCacheKey key, long txTimestamp) throws CacheException {
		try {
			return actualStrategy.get( key, txTimestamp );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return null;
		}
	}

	@Override
	public boolean insert(EntityCacheKey key, Object value, Object version) throws CacheException {
		try {
			return actualStrategy.insert( key, value, version );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public SoftLock lockItem(EntityCacheKey key, Object version) throws CacheException {
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
	public boolean putFromLoad(EntityCacheKey key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
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
	public boolean putFromLoad(EntityCacheKey key, Object value, long txTimestamp, Object version) throws CacheException {
		try {
			return actualStrategy.putFromLoad( key, value, txTimestamp, version );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public void remove(EntityCacheKey key) throws CacheException {
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
	public void unlockItem(EntityCacheKey key, SoftLock lock) throws CacheException {
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

	@Override
	public boolean update(EntityCacheKey key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		try {
			return actualStrategy.update( key, value, currentVersion, previousVersion );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public EntityCacheKey generateCacheKey(Serializable id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.createEntityKey( id, persister, factory, tenantIdentifier );
	}
}

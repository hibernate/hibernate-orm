/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Implementation of {@link NaturalIdRegionAccessStrategy} that handles {@link NonStopCacheException} using
 * {@link HibernateNonstopCacheExceptionHandler}
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class NonstopAwareNaturalIdRegionAccessStrategy implements NaturalIdRegionAccessStrategy {
	private final NaturalIdRegionAccessStrategy actualStrategy;
	private final HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler;

	/**
	 * Constructor accepting the actual {@link NaturalIdRegionAccessStrategy} and the {@link HibernateNonstopCacheExceptionHandler}
	 *
	 * @param actualStrategy The wrapped NaturalIdRegionAccessStrategy
	 * @param hibernateNonstopExceptionHandler The exception handler
	 */
	public NonstopAwareNaturalIdRegionAccessStrategy(
			NaturalIdRegionAccessStrategy actualStrategy,
			HibernateNonstopCacheExceptionHandler hibernateNonstopExceptionHandler) {
		this.actualStrategy = actualStrategy;
		this.hibernateNonstopExceptionHandler = hibernateNonstopExceptionHandler;
	}

	@Override
	public boolean insert(Object key, Object value) throws CacheException {
		try {
			return actualStrategy.insert( key, value );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public boolean afterInsert(Object key, Object value) throws CacheException {
		try {
			return actualStrategy.afterInsert( key, value );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public boolean update(Object key, Object value) throws CacheException {
		try {
			return actualStrategy.update( key, value );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException {
		try {
			return actualStrategy.afterUpdate( key, value, lock );
		}
		catch (NonStopCacheException nonStopCacheException) {
			hibernateNonstopExceptionHandler.handleNonstopCacheException( nonStopCacheException );
			return false;
		}
	}

	@Override
	public NaturalIdRegion getRegion() {
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

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SessionImplementor session) {
		return DefaultCacheKeysFactory.createNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return DefaultCacheKeysFactory.getNaturalIdValues(cacheKey);
	}
}

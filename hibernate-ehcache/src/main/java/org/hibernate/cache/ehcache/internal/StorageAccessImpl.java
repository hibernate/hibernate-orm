/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.hibernate.nonstop.HibernateNonstopCacheExceptionHandler;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.support.StorageAccess;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StorageAccessImpl implements StorageAccess  {
	private static final Logger LOG = Logger.getLogger( StorageAccessImpl.class );

	private final Cache cache;

	public StorageAccessImpl(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}


	@Override
	public Object getFromCache(Object key) {
		try {
			final Element element = getCache().get( key );
			if ( element == null ) {
				return null;
			}
			else {
				return element.getObjectValue();
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
				return null;
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public void putIntoCache(Object key, Object value) {
		try {
			final Element element = new Element( key, value );
			getCache().put( element );
		}
		catch (IllegalArgumentException | IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public void removeFromCache(Object key) {
		try {
			getCache().remove( key );
		}
		catch (ClassCastException | IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public void clearCache() {
		try {
			getCache().removeAll();
		}
		catch (IllegalStateException e) {
			throw new CacheException( e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public void release() {
		try {
			getCache().getCacheManager().removeCache( getCache().getName() );
		}
		catch (IllegalStateException e) {
			//When Spring and Hibernate are both involved this will happen in normal shutdown operation.
			//Do not throw an exception, simply log this one.
			LOG.debug( "This can happen if multiple frameworks both try to shutdown ehcache", e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}
}

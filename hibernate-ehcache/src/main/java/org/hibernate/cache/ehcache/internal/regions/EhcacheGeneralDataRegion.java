/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.GeneralDataRegion;

import org.jboss.logging.Logger;

/**
 * An Ehcache specific GeneralDataRegion.
 * <p/>
 * GeneralDataRegion instances are used for both the timestamps and query caches.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
abstract class EhcacheGeneralDataRegion extends EhcacheDataRegion implements GeneralDataRegion {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			EhcacheGeneralDataRegion.class.getName()
	);

	/**
	 * Constructs an EhcacheGeneralDataRegion around the given underlying cache.
	 *
	 * @param accessStrategyFactory The factory for building needed RegionAccessStrategy instance
	 * @param underlyingCache The ehcache cache instance
	 * @param properties Any additional[ properties
	 */
	public EhcacheGeneralDataRegion(
			EhcacheAccessStrategyFactory accessStrategyFactory,
			Ehcache underlyingCache,
			Properties properties) {
		super( accessStrategyFactory, underlyingCache, properties );
	}

	@Override
	public Object get(Object key) throws CacheException {
		try {
			LOG.debugf( "key: %s", key );
			if ( key == null ) {
				return null;
			}
			else {
				final Element element = getCache().get( key );
				if ( element == null ) {
					LOG.debugf( "Element for key %s is null", key );
					return null;
				}
				else {
					return element.getObjectValue();
				}
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
	public void put(Object key, Object value) throws CacheException {
		LOG.debugf( "key: %s value: %s", key, value );
		try {
			final Element element = new Element( key, value );
			getCache().put( element );
		}
		catch (IllegalArgumentException e) {
			throw new CacheException( e );
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
	public void evict(Object key) throws CacheException {
		try {
			getCache().remove( key );
		}
		catch (ClassCastException e) {
			throw new CacheException( e );
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
	public void evictAll() throws CacheException {
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
}

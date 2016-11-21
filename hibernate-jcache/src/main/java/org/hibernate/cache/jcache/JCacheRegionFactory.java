/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.jcache.time.Timestamper;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;

import org.jboss.logging.Logger;

/**
 * @author Alex Snaps
 */
public class JCacheRegionFactory implements RegionFactory {

	private static final String PROP_PREFIX = "hibernate.javax.cache";

	public static final String PROVIDER = PROP_PREFIX + ".provider";
	public static final String CONFIG_URI = PROP_PREFIX + ".uri";

	private static final JCacheMessageLogger LOG = Logger.getMessageLogger(
			JCacheMessageLogger.class,
			JCacheRegionFactory.class.getName()
	);

	static long nextTS() {
		return Timestamper.next();
	}

	static int timeOut() {
		return (int) TimeUnit.SECONDS.toMillis( 60 ) * Timestamper.ONE_MS;
	}

	private final AtomicBoolean started = new AtomicBoolean( false );
	private volatile CacheManager cacheManager;
	private SessionFactoryOptions options;

	@Override
	public void start(final SessionFactoryOptions options, final Properties properties) throws CacheException {
		if ( started.compareAndSet( false, true ) ) {
			synchronized ( this ) {
				this.options = options;
				try {
					this.cacheManager = getCacheManager( properties );
				}
				finally {
					if ( this.cacheManager == null ) {
						started.set( false );
					}
				}
			}
		}
		else {
			LOG.attemptToRestartAlreadyStartedJCacheProvider();
		}
	}

	@Override
	public void stop() {
		if ( started.compareAndSet( true, false ) ) {
			synchronized ( this ) {
				cacheManager.close();
				cacheManager = null;
			}
		}
		else {
			LOG.attemptToRestopAlreadyStoppedJCacheProvider();
		}
	}

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	public long nextTimestamp() {
		return nextTS();
	}

	@Override
	public EntityRegion buildEntityRegion(final String regionName, final Properties properties, final CacheDataDescription metadata)
			throws CacheException {
		final Cache<Object, Object> cache = getOrCreateCache( regionName, properties, metadata );
		return new JCacheEntityRegion( cache, metadata, options );
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(final String regionName, final Properties properties, final CacheDataDescription metadata)
			throws CacheException {
		final Cache<Object, Object> cache = getOrCreateCache( regionName, properties, metadata );
		return new JCacheNaturalIdRegion( cache, metadata, options );
	}

	@Override
	public CollectionRegion buildCollectionRegion(final String regionName, final Properties properties, final CacheDataDescription metadata)
			throws CacheException {
		final Cache<Object, Object> cache = getOrCreateCache( regionName, properties, metadata );
		return new JCacheCollectionRegion( cache, metadata, options );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(final String regionName, final Properties properties)
			throws CacheException {
		final Cache<Object, Object> cache = getOrCreateCache( regionName, properties, null );
		return new JCacheQueryResultsRegion( cache );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(final String regionName, final Properties properties)
			throws CacheException {
		final Cache<Object, Object> cache = getOrCreateCache( regionName, properties, null );
		return new JCacheTimestampsRegion( cache );
	}

	boolean isStarted() {
		return started.get() && cacheManager != null;
	}

	protected SessionFactoryOptions getOptions() {
		return options;
	}

	protected CachingProvider getCachingProvider(final Properties properties){
		final CachingProvider cachingProvider;
		final String provider = getProp( properties, PROVIDER );
		if ( provider != null ) {
			cachingProvider = Caching.getCachingProvider( provider );
		}
		else {
			cachingProvider = Caching.getCachingProvider();
		}
		return cachingProvider;
	}

	protected CacheManager getCacheManager(final Properties properties){
		final CachingProvider cachingProvider = getCachingProvider( properties );
		final CacheManager cacheManager;
		final String cacheManagerUri = getProp( properties, CONFIG_URI );
		if ( cacheManagerUri != null ) {
			URI uri;
			try {
				uri = new URI( cacheManagerUri );
			}
			catch ( URISyntaxException e ) {
				throw new CacheException( "Couldn't create URI from " + cacheManagerUri, e );
			}
			cacheManager = cachingProvider.getCacheManager( uri, cachingProvider.getDefaultClassLoader() );
		}
		else {
			cacheManager = cachingProvider.getCacheManager();
		}
		return cacheManager;
	}

	protected Cache<Object, Object> getOrCreateCache(String regionName, Properties properties, CacheDataDescription metadata) {
		checkStatus();
		final Cache<Object, Object> cache = cacheManager.getCache( regionName );
		if ( cache == null ) {
			try {
				return cacheManager.createCache( regionName, newDefaultConfig( properties, metadata ) );
			}
			catch ( CacheException e ) {
				final Cache<Object, Object> existing = cacheManager.getCache( regionName );
				if ( existing != null ) {
					return existing;
				}
				throw e;
			}
		}
		return cache;
	}

	protected Configuration<Object, Object> newDefaultConfig(Properties properties, CacheDataDescription metadata) {
		return new MutableConfiguration<Object, Object>();
	}

	protected CacheManager getCacheManager() {
		return cacheManager;
	}

	protected String getProp(Properties properties, String prop) {
		return properties != null ? properties.getProperty( prop ) : null;
	}

	protected void checkStatus() {
		if(!isStarted()) {
			throw new IllegalStateException("JCacheRegionFactory not yet started!");
		}
	}

}

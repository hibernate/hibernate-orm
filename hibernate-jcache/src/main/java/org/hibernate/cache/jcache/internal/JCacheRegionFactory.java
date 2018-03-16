/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.hibernate.engine.spi.SessionFactoryImplementor;

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

	private final AtomicBoolean started = new AtomicBoolean( false );
	private final CacheKeysFactory cacheKeysFactory;
	private volatile CacheManager cacheManager;
	private SessionFactoryOptions options;

	public JCacheRegionFactory() {
		this( DefaultCacheKeysFactory.INSTANCE );
	}

	public JCacheRegionFactory(CacheKeysFactory cacheKeysFactory) {
		this.cacheKeysFactory = cacheKeysFactory;
	}

	protected CacheManager getCacheManager() {
		return cacheManager;
	}

	public CacheKeysFactory determineKeysFactoryToUse(DomainDataRegionBuildingContext buildingContext) {
		return buildingContext.getEnforcedCacheKeysFactory() != null
				? buildingContext.getEnforcedCacheKeysFactory()
				: cacheKeysFactory;
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
		return SimpleTimestamper.next();
	}

	@Override
	public long getTimeout() {
		return SimpleTimestamper.timeOut();
	}

	@Override
	public void start(SessionFactoryOptions settings, Map configValues) throws CacheException {
		if ( started.compareAndSet( false, true ) ) {
			synchronized ( this ) {
				this.options = options;
				try {
					this.cacheManager = getCacheManager( configValues );
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
	public String qualify(String regionName) {
		return RegionNameQualifier.INSTANCE.qualify( regionName, getOptions() );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new QueryResultsRegionImpl(
				regionName,
				this,
				getOrCreateCache( qualify( regionName ) )
		);
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new TimestampsRegionImpl(
				regionName,
				this,
				getOrCreateCache( qualify( regionName ) )
		);
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl(
				regionConfig,
				this,
				getOrCreateCache( qualify( regionConfig.getRegionName() ) ),
				buildingContext
		);
	}

	boolean isStarted() {
		return started.get() && cacheManager != null;
	}

	protected SessionFactoryOptions getOptions() {
		return options;
	}

	protected CachingProvider getCachingProvider(final Map properties){
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

	protected CacheManager getCacheManager(final Map properties){
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

	protected Cache<Object, Object> getOrCreateCache(String qualifiedRegionName) {
		checkStatus();
		assert isQualified( qualifiedRegionName );

		final Cache<Object, Object> cache = cacheManager.getCache( qualifiedRegionName );
		if ( cache == null ) {
			return createCache( qualifiedRegionName );
		}
		return cache;
	}

	private boolean isQualified(String regionName) {
		final String prefix = options.getCacheRegionPrefix();
		if ( prefix == null ) {
			return true;
		}
		else {
			return regionName.startsWith( prefix );
		}
	}

	protected Cache<Object, Object> createCache(String regionName) {
		throw new CacheException( "On-the-fly creation of JCache Cache objects is not supported" );
	}

	protected String getProp(Map properties, String prop) {
		return properties != null ? (String) properties.get( prop ) : null;
	}

	protected void checkStatus() {
		if ( ! isStarted() ) {
			throw new IllegalStateException( "JCacheRegionFactory not yet started!" );
		}
	}
}

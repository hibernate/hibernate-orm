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

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.ConfigSettings;
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

	public CacheManager getCacheManager() {
		return cacheManager;
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
				this.options = settings;
				try {
					this.cacheManager = getCacheManager( settings, configValues );
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

	protected CacheManager getCacheManager(SessionFactoryOptions settings, Map properties) {
		final Object explicitCacheManager = properties.get( ConfigSettings.CACHE_MANAGER );
		if ( explicitCacheManager != null ) {
			return useExplicitCacheManager( settings, explicitCacheManager );
		}

		final CachingProvider cachingProvider = getCachingProvider( properties );
		final CacheManager cacheManager;
		final String cacheManagerUri = getProp( properties, ConfigSettings.CONFIG_URI );
		if ( cacheManagerUri != null ) {
			URI uri;
			try {
				uri = new URI( cacheManagerUri );
			}
			catch ( URISyntaxException e ) {
				throw new CacheException( "Couldn't create URI from " + cacheManagerUri, e );
			}
			// todo (5.3) : shouldn't this use Hibernate's AggregatedClassLoader?
			cacheManager = cachingProvider.getCacheManager( uri, cachingProvider.getDefaultClassLoader() );
		}
		else {
			cacheManager = cachingProvider.getCacheManager();
		}
		return cacheManager;
	}

	protected CachingProvider getCachingProvider(final Map properties){
		final CachingProvider cachingProvider;
		final String provider = getProp( properties, ConfigSettings.PROVIDER );
		if ( provider != null ) {
			cachingProvider = Caching.getCachingProvider( provider );
		}
		else {
			cachingProvider = Caching.getCachingProvider();
		}
		return cachingProvider;
	}

	private CacheManager useExplicitCacheManager(SessionFactoryOptions settings, Object setting) {
		if ( setting instanceof CacheManager ) {
			return (CacheManager) setting;
		}

		final Class<? extends CacheManager> cacheManagerClass;
		if ( setting instanceof Class ) {
			cacheManagerClass = (Class<? extends CacheManager>) setting;
		}
		else {
			cacheManagerClass = settings.getServiceRegistry().getService( ClassLoaderService.class )
					.classForName( setting.toString() );
		}

		try {
			return cacheManagerClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new CacheException( "Could not use explicit CacheManager : " + setting );
		}
	}

	@Override
	public void stop() {
		if ( started.compareAndSet( true, false ) ) {
			synchronized ( this ) {
				// todo (5.3) : if this is a manager instance that was provided to us we should probably not close it...
				//		- when the explicit `setting` passed to `#useExplicitCacheManager` is
				//		a CacheManager instance
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
		return RegionNameQualifier.INSTANCE.qualify( regionName, options );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new QueryResultsRegionImpl(
				regionName,
				this,
				getOrCreateCache( regionName, sessionFactory )
		);
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new TimestampsRegionImpl(
				regionName,
				this,
				getOrCreateCache( regionName, sessionFactory )
		);
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl(
				regionConfig,
				this,
				getOrCreateCache( regionConfig.getRegionName(), buildingContext.getSessionFactory() ),
				cacheKeysFactory,
				buildingContext
		);
	}

	protected Cache<Object, Object> getOrCreateCache(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		checkStatus();
		assert !RegionNameQualifier.INSTANCE.isQualified( unqualifiedRegionName, sessionFactory.getSessionFactoryOptions() );

		final String qualifiedRegionName = RegionNameQualifier.INSTANCE.qualify(
				unqualifiedRegionName,
				sessionFactory.getSessionFactoryOptions()
		);

		final Cache<Object, Object> cache = cacheManager.getCache( qualifiedRegionName );
		if ( cache == null ) {
			return createCache( qualifiedRegionName );
		}
		return cache;
	}

	protected Cache<Object, Object> createCache(String regionName) {
		throw new CacheException( "On-the-fly creation of JCache Cache objects is not supported [" + regionName + "]" );
	}

	protected String getProp(Map properties, String prop) {
		return properties != null ? (String) properties.get( prop ) : null;
	}

	protected void checkStatus() {
		if ( ! isStarted() ) {
			throw new IllegalStateException( "JCacheRegionFactory not yet started!" );
		}
	}

	boolean isStarted() {
		return started.get() && cacheManager != null;
	}
}

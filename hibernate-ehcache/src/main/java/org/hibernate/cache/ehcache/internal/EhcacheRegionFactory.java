/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.ehcache.ConfigSettings;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cache.ehcache.ConfigSettings.EHCACHE_CONFIGURATION_RESOURCE_NAME;

/**
 * @author Steve Ebersole
 * @author Alex Snaps
 */
public class EhcacheRegionFactory implements RegionFactory {
	private static final Logger LOG = Logger.getLogger( EhcacheRegionFactory.class );

	private final AtomicBoolean started = new AtomicBoolean( false );
	private final CacheKeysFactory cacheKeysFactory;

	private volatile CacheManager cacheManager;
	private SessionFactoryOptions options;

	public EhcacheRegionFactory() {
		this( DefaultCacheKeysFactory.INSTANCE );
	}

	public EhcacheRegionFactory(CacheKeysFactory cacheKeysFactory) {
		this.cacheKeysFactory = cacheKeysFactory;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public SessionFactoryOptions getOptions() {
		return options;
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
			SecondLevelCacheLogger.INSTANCE.attemptToStartAlreadyStartedCacheProvider();
		}
	}

	private CacheManager getCacheManager(SessionFactoryOptions settings, Map properties) {
		final Object explicitCacheManager = properties.get( ConfigSettings.CACHE_MANAGER );
		if ( explicitCacheManager != null ) {
			return useExplicitCacheManager( settings, explicitCacheManager );
		}

		return useNormalCacheManager( settings, properties );
	}

	protected CacheManager resolveCacheManager(SessionFactoryOptions settings, Map properties) {
		return useNormalCacheManager( settings, properties );
	}

	/**
	 * Locate the CacheManager during start-up.  protected to allow for subclassing
	 * such as SingletonEhcacheRegionFactory
	 */
	protected static CacheManager useNormalCacheManager(SessionFactoryOptions settings, Map properties) {
		try {
			String configurationResourceName = null;
			if ( properties != null ) {
				configurationResourceName = (String) properties.get( EHCACHE_CONFIGURATION_RESOURCE_NAME );
			}
			if ( configurationResourceName == null || configurationResourceName.length() == 0 ) {
				final Configuration configuration = ConfigurationFactory.parseConfiguration();
				return new CacheManager( configuration );
			}
			else {
				final URL url = loadResource( configurationResourceName, settings );
				final Configuration configuration = HibernateEhcacheUtils.loadAndCorrectConfiguration( url );
				return new CacheManager( configuration );
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e.getMessage().startsWith(
					"Cannot parseConfiguration CacheManager. Attempt to create a new instance of " +
							"CacheManager using the diskStorePath"
			) ) {
				throw new CacheException(
						"Attempt to restart an already started EhCacheRegionFactory. " +
								"Use sessionFactory.close() between repeated calls to buildSessionFactory. " +
								"Consider using SingletonEhCacheRegionFactory. Error from ehcache was: " + e.getMessage()
				);
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	private static URL loadResource(String configurationResourceName, SessionFactoryOptions settings) {
		URL url = settings.getServiceRegistry()
				.getService( ClassLoaderService.class )
				.locateResource( configurationResourceName );

		if ( url == null ) {
			final ClassLoader standardClassloader = Thread.currentThread().getContextClassLoader();
			if ( standardClassloader != null ) {
				url = standardClassloader.getResource( configurationResourceName );
			}
			if ( url == null ) {
				url = EhcacheRegionFactory.class.getResource( configurationResourceName );
			}
			if ( url == null ) {
				try {
					url = new URL( configurationResourceName );
				}
				catch ( MalformedURLException e ) {
					// ignore
				}
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Creating EhCacheRegionFactory from a specified resource: %s.  Resolved to URL: %s",
					configurationResourceName,
					url
			);
		}
		if ( url == null ) {
			EhCacheMessageLogger.INSTANCE.unableToLoadConfiguration( configurationResourceName );
		}

		return url;
	}

	/**
	 * Load a resource from the classpath.
	 */
	protected URL loadResource(String configurationResourceName) {
		if ( ! isStarted() ) {
			throw new IllegalStateException( "Cannot load resource through a non-started EhcacheRegionFactory" );
		}

		return loadResource( configurationResourceName, options );
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
				releaseCacheManager();
				cacheManager = null;
			}
		}
		else {
			SecondLevelCacheLogger.INSTANCE.attemptToStopAlreadyStoppedCacheProvider();
		}
	}

	protected void releaseCacheManager() {
		// todo (5.3) : if this is a manager instance that was provided to us we should probably not close it...
		//		- when the explicit `setting` passed to `#useExplicitCacheManager` is
		//		a CacheManager instance
		cacheManager.shutdown();
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

	protected Cache getOrCreateCache(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		checkStatus();
		assert !RegionNameQualifier.INSTANCE.isQualified( unqualifiedRegionName, sessionFactory.getSessionFactoryOptions() );

		final String qualifiedRegionName = RegionNameQualifier.INSTANCE.qualify(
				unqualifiedRegionName,
				sessionFactory.getSessionFactoryOptions()
		);

		final Cache cache = cacheManager.getCache( qualifiedRegionName );
		if ( cache == null ) {
			return createCache( qualifiedRegionName );
		}
		return cache;
	}

	protected Cache createCache(String regionName) {
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

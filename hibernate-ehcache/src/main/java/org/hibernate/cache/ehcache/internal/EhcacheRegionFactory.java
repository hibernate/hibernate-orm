/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.ehcache.ConfigSettings;
import org.hibernate.cache.ehcache.MissingCacheStrategy;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.hibernate.cache.ehcache.ConfigSettings.EHCACHE_CONFIGURATION_RESOURCE_NAME;
import static org.hibernate.cache.ehcache.internal.HibernateEhcacheUtils.setCacheManagerNameIfNeeded;

/**
 * @author Steve Ebersole
 * @author Alex Snaps
 */
public class EhcacheRegionFactory extends RegionFactoryTemplate {
	private static final EhCacheMessageLogger LOG = EhCacheMessageLogger.INSTANCE;

	private final CacheKeysFactory cacheKeysFactory;

	private volatile CacheManager cacheManager;
	private volatile MissingCacheStrategy missingCacheStrategy;
	private volatile long cacheLockTimeout;

	public EhcacheRegionFactory() {
		this( DefaultCacheKeysFactory.INSTANCE );
	}

	public EhcacheRegionFactory(CacheKeysFactory cacheKeysFactory) {
		this.cacheKeysFactory = cacheKeysFactory;
		DeprecationLogger.INSTANCE.logDeprecation();
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	@Override
	protected CacheKeysFactory getImplicitCacheKeysFactory() {
		return cacheKeysFactory;
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig, DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl(
				regionConfig,
				this,
				createDomainDataStorageAccess( regionConfig, buildingContext ),
				cacheKeysFactory,
				buildingContext
		);
	}

	@Override
	protected DomainDataStorageAccess createDomainDataStorageAccess(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		return new StorageAccessImpl(
				getOrCreateCache( regionConfig.getRegionName(), buildingContext.getSessionFactory() )
		);
	}

	@Override
	protected StorageAccess createQueryResultsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		String defaultedRegionName = defaultRegionName(
				regionName,
				sessionFactory,
				DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME,
				LEGACY_QUERY_RESULTS_REGION_UNQUALIFIED_NAMES
		);
		return new StorageAccessImpl( getOrCreateCache( defaultedRegionName, sessionFactory ) );
	}

	@Override
	protected StorageAccess createTimestampsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		String defaultedRegionName = defaultRegionName(
				regionName,
				sessionFactory,
				DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME,
				LEGACY_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAMES
		);
		return new StorageAccessImpl( getOrCreateCache( defaultedRegionName, sessionFactory ) );
	}

	protected final String defaultRegionName(String regionName, SessionFactoryImplementor sessionFactory,
			String defaultRegionName, List<String> legacyDefaultRegionNames) {
		if ( defaultRegionName.equals( regionName )
				&& !cacheExists( regionName, sessionFactory ) ) {
			// Maybe the user configured caches explicitly with legacy names; try them and use the first that exists

			for ( String legacyDefaultRegionName : legacyDefaultRegionNames ) {
				if ( cacheExists( legacyDefaultRegionName, sessionFactory ) ) {
					SecondLevelCacheLogger.INSTANCE.usingLegacyCacheName( defaultRegionName, legacyDefaultRegionName );
					return legacyDefaultRegionName;
				}
			}
		}

		return regionName;
	}

	protected Ehcache getOrCreateCache(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		verifyStarted();
		assert !RegionNameQualifier.INSTANCE.isQualified( unqualifiedRegionName, sessionFactory.getSessionFactoryOptions() );

		final String qualifiedRegionName = RegionNameQualifier.INSTANCE.qualify(
				unqualifiedRegionName,
				sessionFactory.getSessionFactoryOptions()
		);

		final Ehcache cache = cacheManager.getEhcache( qualifiedRegionName );
		if ( cache == null ) {
			return createCache( qualifiedRegionName );
		}
		return cache;
	}

	protected Ehcache createCache(String regionName) {
		switch ( missingCacheStrategy ) {
			case CREATE_WARN:
				SecondLevelCacheLogger.INSTANCE.missingCacheCreated(
						regionName,
						ConfigSettings.MISSING_CACHE_STRATEGY, MissingCacheStrategy.CREATE.getExternalRepresentation()
				);
				cacheManager.addCache( regionName );
				return cacheManager.getEhcache( regionName );
			case CREATE:
				cacheManager.addCache( regionName );
				return cacheManager.getEhcache( regionName );
			case FAIL:
				throw new CacheException( "On-the-fly creation of Ehcache Cache objects is not supported [" + regionName + "]" );
			default:
				throw new IllegalStateException( "Unsupported missing cache strategy: " + missingCacheStrategy );
		}
	}

	protected boolean cacheExists(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		final String qualifiedRegionName = RegionNameQualifier.INSTANCE.qualify(
				unqualifiedRegionName,
				sessionFactory.getSessionFactoryOptions()
		);
		return cacheManager.getEhcache( qualifiedRegionName ) != null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lifecycle

	@Override
	protected boolean isStarted() {
		return super.isStarted() && cacheManager != null;
	}

	@Override
	protected void prepareForUse(SessionFactoryOptions settings, Map configValues) {
		synchronized ( this ) {
			this.cacheManager = resolveCacheManager( settings, configValues );
			if ( this.cacheManager == null ) {
				throw new CacheException( "Could not start Ehcache CacheManager" );
			}
			this.missingCacheStrategy = MissingCacheStrategy.interpretSetting(
					configValues.get( ConfigSettings.MISSING_CACHE_STRATEGY )
			);

			Object cacheLockTimeoutConfigValue = configValues.get(
				ConfigSettings.EHCACHE_CONFIGURATION_CACHE_LOCK_TIMEOUT
			);
			if ( cacheLockTimeoutConfigValue != null ) {
				Integer lockTimeoutInMillis = null;
				if ( cacheLockTimeoutConfigValue instanceof String ) {
					lockTimeoutInMillis = Integer.decode( (String) cacheLockTimeoutConfigValue );
				}
				else if ( cacheLockTimeoutConfigValue instanceof Number ) {
					lockTimeoutInMillis = ( (Number) cacheLockTimeoutConfigValue ).intValue();
				}
				if ( lockTimeoutInMillis != null ) {
					this.cacheLockTimeout = SimpleTimestamper.ONE_MS * lockTimeoutInMillis;
				}
				else {
					this.cacheLockTimeout = super.getTimeout();
				}
			}
		}
	}

	protected CacheManager resolveCacheManager(SessionFactoryOptions settings, Map properties) {
		final Object explicitCacheManager = properties.get( ConfigSettings.CACHE_MANAGER );
		if ( explicitCacheManager != null ) {
			return useExplicitCacheManager( settings, explicitCacheManager );
		}

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
				setCacheManagerNameIfNeeded( settings, configuration, properties );
				return new CacheManager( configuration );
			}
			else {
				final URL url = loadResource( configurationResourceName, settings );
				final Configuration configuration = HibernateEhcacheUtils.loadAndCorrectConfiguration( url );
				setCacheManagerNameIfNeeded( settings, configuration, properties );
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
		// we use this method to create the cache manager so we can't check it is non null
		// calling the super method then
		if ( ! super.isStarted() ) {
			throw new IllegalStateException( "Cannot load resource through a non-started EhcacheRegionFactory" );
		}

		return loadResource( configurationResourceName, getOptions() );
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
	protected void releaseFromUse() {
		try {
			// todo (5.3) : if this is a manager instance that was provided to us we should probably not close it...
			//		- when the explicit `setting` passed to `#useExplicitCacheManager` is
			//		a CacheManager instance
			cacheManager.shutdown();
		}
		finally {
			cacheManager = null;
		}
	}

	@Override
	public long getTimeout() {
		return cacheLockTimeout;
	}
}

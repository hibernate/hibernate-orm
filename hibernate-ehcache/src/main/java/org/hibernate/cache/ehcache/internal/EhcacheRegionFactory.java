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
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cache.ehcache.ConfigSettings.EHCACHE_CONFIGURATION_RESOURCE_NAME;
import static org.hibernate.cache.ehcache.internal.HibernateEhcacheUtils.setCacheManagerNameIfNeeded;

/**
 * @author Steve Ebersole
 * @author Alex Snaps
 */
public class EhcacheRegionFactory extends RegionFactoryTemplate {
	private static final Logger LOG = Logger.getLogger( EhcacheRegionFactory.class );

	private final CacheKeysFactory cacheKeysFactory;

	private volatile CacheManager cacheManager;

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
		return new StorageAccessImpl( getOrCreateCache( regionName, sessionFactory ) );
	}

	@Override
	protected StorageAccess createTimestampsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new StorageAccessImpl( getOrCreateCache( regionName, sessionFactory ) );
	}

	protected Cache getOrCreateCache(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		verifyStarted();
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lifecycle

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
		if ( ! isStarted() ) {
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
}

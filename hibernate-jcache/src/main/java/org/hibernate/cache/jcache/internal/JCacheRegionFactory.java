/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cache.jcache.MissingCacheStrategy;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Alex Snaps
 */
public class JCacheRegionFactory extends RegionFactoryTemplate {
	private final CacheKeysFactory cacheKeysFactory;

	private volatile CacheManager cacheManager;
	private volatile MissingCacheStrategy missingCacheStrategy;

	@SuppressWarnings("unused")
	public JCacheRegionFactory() {
		this( DefaultCacheKeysFactory.INSTANCE );
	}

	public JCacheRegionFactory(CacheKeysFactory cacheKeysFactory) {
		this.cacheKeysFactory = cacheKeysFactory;
	}

	@SuppressWarnings("unused")
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
		return new JCacheDomainDataRegionImpl(
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
		return new JCacheAccessImpl(
				getOrCreateCache( regionConfig.getRegionName(), buildingContext.getSessionFactory() )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected Cache<Object, Object> getOrCreateCache(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		verifyStarted();
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

	@SuppressWarnings("WeakerAccess")
	protected Cache<Object, Object> createCache(String regionName) {
		switch ( missingCacheStrategy ) {
			case CREATE_WARN:
				SecondLevelCacheLogger.INSTANCE.missingCacheCreated(
						regionName,
						ConfigSettings.MISSING_CACHE_STRATEGY, MissingCacheStrategy.CREATE.getExternalRepresentation()
				);
				return cacheManager.createCache( regionName, new MutableConfiguration<>() );
			case CREATE:
				return cacheManager.createCache( regionName, new MutableConfiguration<>() );
			case FAIL:
				throw new CacheException( "On-the-fly creation of JCache Cache objects is not supported [" + regionName + "]" );
			default:
				throw new IllegalStateException( "Unsupported missing cache strategy: " + missingCacheStrategy );
		}
	}

	protected boolean cacheExists(String unqualifiedRegionName, SessionFactoryImplementor sessionFactory) {
		final String qualifiedRegionName = RegionNameQualifier.INSTANCE.qualify(
				unqualifiedRegionName,
				sessionFactory.getSessionFactoryOptions()
		);
		return cacheManager.getCache( qualifiedRegionName ) != null;
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
		return new JCacheAccessImpl(
				getOrCreateCache( defaultedRegionName, sessionFactory )
		);
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
		return new JCacheAccessImpl(
				getOrCreateCache( defaultedRegionName, sessionFactory )
		);
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



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lifecycle

	@Override
	protected boolean isStarted() {
		return super.isStarted() && cacheManager != null;
	}

	@Override
	protected void prepareForUse(SessionFactoryOptions settings, Map configValues) {
		this.cacheManager = resolveCacheManager( settings, configValues );
		if ( this.cacheManager == null ) {
			throw new CacheException( "Could not locate/create CacheManager" );
		}
		this.missingCacheStrategy = MissingCacheStrategy.interpretSetting(
				getProp( configValues, ConfigSettings.MISSING_CACHE_STRATEGY )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected CacheManager resolveCacheManager(SessionFactoryOptions settings, Map properties) {
		final Object explicitCacheManager = properties.get( ConfigSettings.CACHE_MANAGER );
		if ( explicitCacheManager != null ) {
			return useExplicitCacheManager( settings, explicitCacheManager );
		}

		final CachingProvider cachingProvider = getCachingProvider( properties );
		final CacheManager cacheManager;
		final URI cacheManagerUri = getUri( settings, properties );
		if ( cacheManagerUri != null ) {
			cacheManager = cachingProvider.getCacheManager( cacheManagerUri, getClassLoader( cachingProvider ) );
		}
		else {
			cacheManager = cachingProvider.getCacheManager( cachingProvider.getDefaultURI(), getClassLoader( cachingProvider ) );
		}
		return cacheManager;
	}

	@SuppressWarnings("WeakerAccess")
	protected ClassLoader getClassLoader(CachingProvider cachingProvider) {
		// todo (5.3) : shouldn't this use Hibernate's AggregatedClassLoader?
		return cachingProvider.getDefaultClassLoader();
	}

	protected URI getUri(SessionFactoryOptions settings, Map properties) {
		String cacheManagerUri = getProp( properties, ConfigSettings.CONFIG_URI );
		if ( cacheManagerUri == null ) {
			return null;
		}

		URL url = settings.getServiceRegistry()
				.getService( ClassLoaderService.class )
				.locateResource( cacheManagerUri );

		if ( url == null ) {
			throw new CacheException( "Couldn't load URI from " + cacheManagerUri );
		}

		try {
			return url.toURI();
		}
		catch (URISyntaxException e) {
			throw new CacheException( "Couldn't load URI from " + cacheManagerUri, e );
		}
	}

	private String getProp(Map properties, String prop) {
		return properties != null ? (String) properties.get( prop ) : null;
	}

	@SuppressWarnings("WeakerAccess")
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

	@SuppressWarnings("unchecked")
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
			cacheManager.close();
		}
		finally {
			cacheManager = null;
		}
	}
}

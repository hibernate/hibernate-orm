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
		throw new CacheException( "On-the-fly creation of JCache Cache objects is not supported [" + regionName + "]" );
	}

	@Override
	protected StorageAccess createQueryResultsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new JCacheAccessImpl(
				getOrCreateCache( regionName, sessionFactory )
		);
	}

	@Override
	protected StorageAccess createTimestampsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new JCacheAccessImpl(
				getOrCreateCache( regionName, sessionFactory )
		);
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
	}

	@SuppressWarnings("WeakerAccess")
	protected CacheManager resolveCacheManager(SessionFactoryOptions settings, Map properties) {
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

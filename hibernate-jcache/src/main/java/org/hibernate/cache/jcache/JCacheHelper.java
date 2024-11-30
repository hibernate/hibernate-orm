/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.jcache;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

/**
 * @author Steve Ebersole
 */
public class JCacheHelper {

	/**
	 * @apiNote Access to the standard CacheManager used by `hibernate-jcache` unless
	 * configured differently.
	 */
	public static CacheManager locateStandardCacheManager() {
		// unless configured differently, this is how JCacheRegionFactory
		//		will locate the CacheManager to use

		CachingProvider cachingProvider = Caching.getCachingProvider();

		// JRegionFactory::prepareForUse retrieves the class loader service from
		// the service registry and registers it as the
		// Since EHCache by itself doesn't use this class loader by itself, it needs to be injected here.
		// It is set via JCacheRegionFactory::prepareForUse. S.a. https://github.com/ehcache/ehcache3/issues/3252
		return cachingProvider.getCacheManager( cachingProvider.getDefaultURI(), Caching.getDefaultClassLoader() );
	}
}

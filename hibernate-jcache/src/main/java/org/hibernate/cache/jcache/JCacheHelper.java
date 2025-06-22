/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.jcache;

import javax.cache.CacheManager;
import javax.cache.Caching;

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
		return Caching.getCachingProvider().getCacheManager();
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

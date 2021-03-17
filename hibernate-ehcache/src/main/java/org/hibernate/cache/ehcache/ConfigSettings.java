/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache;

import net.sf.ehcache.CacheManager;

/**
 * @author Steve Ebersole
 */
public interface ConfigSettings {
	String PROP_PREFIX = "hibernate.cache.ehcache.";

	/**
	 * Allows providing `hibernate-ehcache` with a custom Ehcache {@link CacheManager}.
	 */
	String CACHE_MANAGER = PROP_PREFIX + "cache_manager";

	/**
	 * Define the behavior of the region factory when a cache is missing,
	 * i.e. when the cache was not created by the cache manager as it started.
	 *
	 * See {@link MissingCacheStrategy} for the various possible values.
	 *
	 * Default value is {@link MissingCacheStrategy#FAIL}.
	 */
	String MISSING_CACHE_STRATEGY = PROP_PREFIX + "missing_cache_strategy";

	/**
	 * This is the legacy property name.  No need to change it to fit under {@link #PROP_PREFIX}
	 */
	String EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

	String EHCACHE_CONFIGURATION_CACHE_MANAGER_NAME = "net.sf.ehcache.cacheManagerName";

	String EHCACHE_CONFIGURATION_CACHE_LOCK_TIMEOUT = "net.sf.ehcache.hibernate.cache_lock_timeout";
}

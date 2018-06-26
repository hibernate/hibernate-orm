/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache;

import java.net.URI;
import javax.cache.CacheManager;

/**
 * @author Steve Ebersole
 */
public interface ConfigSettings {
	String SIMPLE_FACTORY_NAME = "jcache";

	String PROP_PREFIX = "hibernate.javax.cache.";

	/**
	 * Allows providing `hibernate-jcache` with a custom JCache {@link CacheManager}.
	 */
	String CACHE_MANAGER = PROP_PREFIX + "cache_manager";

	/**
	 * Allows providing `hibernate-jcache` with a custom JCache {@link javax.cache.spi.CachingProvider}.
	 */
	String PROVIDER = PROP_PREFIX + "provider";

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
	 * Designates the URI for a specific JCache {@link CacheManager} JCacheRegionFactory
	 * should ask the {@link javax.cache.spi.CachingProvider} for
	 *
	 * @see javax.cache.spi.CachingProvider#getCacheManager(URI, ClassLoader)
	 */
	String CONFIG_URI = PROP_PREFIX + "uri";
}

//$Id$
package org.hibernate.cache;

import java.util.Properties;

/**
 * Support for pluggable caches.
 *
 * @author Gavin King
 */
public interface CacheProvider {

	/**
	 * Configure the cache
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 * @throws CacheException
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException;

	/**
	 * Generate a timestamp
	 */
	public long nextTimestamp();

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation
	 * during SessionFactory construction.
	 *
	 * @param properties current configuration settings.
	 */
	public void start(Properties properties) throws CacheException;

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation
	 * during SessionFactory.close().
	 */
	public void stop();
	
	public boolean isMinimalPutsEnabledByDefault();

}

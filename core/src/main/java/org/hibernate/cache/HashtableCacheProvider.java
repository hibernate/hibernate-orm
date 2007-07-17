//$Id: HashtableCacheProvider.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.cache;

import java.util.Properties;

/**
 * A simple in-memory Hashtable-based cache impl.
 * 
 * @author Gavin King
 */
public class HashtableCacheProvider implements CacheProvider {

	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		return new HashtableCache( regionName );
	}

	public long nextTimestamp() {
		return Timestamper.next();
	}

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation
	 * during SessionFactory construction.
	 *
	 * @param properties current configuration settings.
	 */
	public void start(Properties properties) throws CacheException {
	}

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation
	 * during SessionFactory.close().
	 */
	public void stop() {
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

}


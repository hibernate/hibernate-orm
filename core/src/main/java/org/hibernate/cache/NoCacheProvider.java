// $Id: NoCacheProvider.java 6433 2005-04-15 18:20:03Z steveebersole $
package org.hibernate.cache;

import java.util.Properties;

/**
 * Implementation of NoCacheProvider.
 *
 * @author Steve Ebersole
 */
public class NoCacheProvider implements CacheProvider {
	/**
	 * Configure the cache
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 *
	 * @throws CacheException
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		throw new NoCachingEnabledException();
	}

	/**
	 * Generate a timestamp
	 */
	public long nextTimestamp() {
		// This, is used by SessionFactoryImpl to hand to the generated SessionImpl;
		// was the only reason I could see that we cannot just use null as
		// Settings.cacheProvider
		return System.currentTimeMillis() / 100;
	}

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation during SessionFactory
	 * construction.
	 *
	 * @param properties current configuration settings.
	 */
	public void start(Properties properties) throws CacheException {
		// this is called by SessionFactory irregardless; we just disregard here;
		// could also add a check to SessionFactory to only conditionally call start
	}

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation during SessionFactory.close().
	 */
	public void stop() {
		// this is called by SessionFactory irregardless; we just disregard here;
		// could also add a check to SessionFactory to only conditionally call stop
	}

	public boolean isMinimalPutsEnabledByDefault() {
		// this is called from SettingsFactory irregardless; trivial to simply disregard
		return false;
	}

}

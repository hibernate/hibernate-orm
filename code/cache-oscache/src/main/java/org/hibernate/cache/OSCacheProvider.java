//$Id: OSCacheProvider.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.cache;

import java.util.Properties;

import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.StringHelper;

import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.base.Config;

/**
 * Support for OpenSymphony OSCache. This implementation assumes
 * that identifiers have well-behaved <tt>toString()</tt> methods.
 *
 * @author <a href="mailto:m.bogaert@intrasoft.be">Mathias Bogaert</a>
 */
public class OSCacheProvider implements CacheProvider {

	/**
	 * The <tt>OSCache</tt> refresh period property suffix.
	 */
	public static final String OSCACHE_REFRESH_PERIOD = "refresh.period";
	/**
	 * The <tt>OSCache</tt> CRON expression property suffix.
	 */
	public static final String OSCACHE_CRON = "cron";
	/**
	 * The <tt>OSCache</tt> cache capacity property suffix.
	 */
	public static final String OSCACHE_CAPACITY = "capacity";

	private static final Properties OSCACHE_PROPERTIES = new Config().getProperties();

	/**
	 * Builds a new {@link Cache} instance, and gets it's properties from the OSCache {@link Config}
	 * which reads the properties file (<code>oscache.properties</code>) from the classpath.
	 * If the file cannot be found or loaded, an the defaults are used.
	 *
	 * @param region
	 * @param properties
	 * @return
	 * @throws CacheException
	 */
	public Cache buildCache(String region, Properties properties) throws CacheException {

		int refreshPeriod = PropertiesHelper.getInt(
			StringHelper.qualify(region, OSCACHE_REFRESH_PERIOD),
			OSCACHE_PROPERTIES,
			CacheEntry.INDEFINITE_EXPIRY
		);
		String cron = OSCACHE_PROPERTIES.getProperty( StringHelper.qualify(region, OSCACHE_CRON) );

		// construct the cache
		final OSCache cache = new OSCache(refreshPeriod, cron, region);

		Integer capacity = PropertiesHelper.getInteger( StringHelper.qualify(region, OSCACHE_CAPACITY), OSCACHE_PROPERTIES );
		if ( capacity!=null ) cache.setCacheCapacity( capacity.intValue() );

		return cache;
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import java.net.URL;
import java.util.Map;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;

import org.hibernate.boot.spi.SessionFactoryOptions;

import static org.hibernate.cache.ehcache.ConfigSettings.EHCACHE_CONFIGURATION_CACHE_MANAGER_NAME;


/**
 * Copy of Ehcache utils into Hibernate code base
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public final class HibernateEhcacheUtils {

	private HibernateEhcacheUtils() {
	}

	/**
	 * Create a cache manager configuration from the supplied url, correcting it for Hibernate compatibility.
	 * <p/>
	 * Currently "correcting" for Hibernate compatibility means simply switching any identity based value modes
	 * to serialization.
	 *
	 * @param url The url to load the config from
	 *
	 * @return The Ehcache Configuration object
	 */
	public static Configuration loadAndCorrectConfiguration(URL url) {
		final Configuration config = ConfigurationFactory.parseConfiguration( url );
		
		// EHC-875 / HHH-6576
		if ( config == null ) {
			return null;
		}

		if ( config.getDefaultCacheConfiguration() != null
				&& config.getDefaultCacheConfiguration().isTerracottaClustered() ) {
			setupHibernateTimeoutBehavior(
					config.getDefaultCacheConfiguration()
							.getTerracottaConfiguration()
							.getNonstopConfiguration()
			);
		}

		for ( CacheConfiguration cacheConfig : config.getCacheConfigurations().values() ) {
			if ( cacheConfig.isTerracottaClustered() ) {
				setupHibernateTimeoutBehavior( cacheConfig.getTerracottaConfiguration().getNonstopConfiguration() );
			}
		}
		return config;
	}

	static void setCacheManagerNameIfNeeded(SessionFactoryOptions settings, Configuration configuration, Map properties) {
		overwriteCacheManagerIfConfigured( configuration, properties );
		if (configuration.getName() == null) {
			String sessionFactoryName = settings.getSessionFactoryName();
			if (sessionFactoryName != null) {
				configuration.setName( sessionFactoryName );
			}
			else {
				configuration.setName( "Hibernate " + settings.getUuid() );
			}
		}
	}

	static Configuration overwriteCacheManagerIfConfigured(final Configuration configuration, final Map properties) {
		if (properties != null) {
			final String cacheManagerName = (String) properties.get( EHCACHE_CONFIGURATION_CACHE_MANAGER_NAME );
			if (cacheManagerName != null) {
				configuration.setName( cacheManagerName );
			}
		}
		return configuration;
	}

	private static void setupHibernateTimeoutBehavior(NonstopConfiguration nonstopConfig) {
		nonstopConfig.getTimeoutBehavior().setType( TimeoutBehaviorType.EXCEPTION.getTypeName() );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.ehcache.internal.util;

import java.net.URL;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;

import org.jboss.logging.Logger;


/**
 * Copy of Ehcache utils into Hibernate code base
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public final class HibernateEhcacheUtils {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			HibernateEhcacheUtils.class.getName()
	);

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
			if ( ValueMode.IDENTITY
					.equals( config.getDefaultCacheConfiguration().getTerracottaConfiguration().getValueMode() ) ) {
				LOG.incompatibleCacheValueMode();
				config.getDefaultCacheConfiguration()
						.getTerracottaConfiguration()
						.setValueMode( ValueMode.SERIALIZATION.name() );
			}
			setupHibernateTimeoutBehavior(
					config.getDefaultCacheConfiguration()
							.getTerracottaConfiguration()
							.getNonstopConfiguration()
			);
		}

		for ( CacheConfiguration cacheConfig : config.getCacheConfigurations().values() ) {
			if ( cacheConfig.isTerracottaClustered() ) {
				if ( ValueMode.IDENTITY.equals( cacheConfig.getTerracottaConfiguration().getValueMode() ) ) {
					LOG.incompatibleCacheValueModePerCache( cacheConfig.getName() );
					cacheConfig.getTerracottaConfiguration().setValueMode( ValueMode.SERIALIZATION.name() );
				}
				setupHibernateTimeoutBehavior( cacheConfig.getTerracottaConfiguration().getNonstopConfiguration() );
			}
		}
		return config;
	}

	private static void setupHibernateTimeoutBehavior(NonstopConfiguration nonstopConfig) {
		nonstopConfig.getTimeoutBehavior().setType( TimeoutBehaviorType.EXCEPTION.getTypeName() );
	}

	/**
	 * Validates that the supplied Ehcache instance is valid for use as a Hibernate cache.
	 *
	 * @param cache The cache instance
	 *
	 * @throws CacheException If any explicit settings on the cache are not validate
	 */
	public static void validateEhcache(Ehcache cache) throws CacheException {
		final CacheConfiguration cacheConfig = cache.getCacheConfiguration();

		if ( cacheConfig.isTerracottaClustered() ) {
			final TerracottaConfiguration tcConfig = cacheConfig.getTerracottaConfiguration();
			switch ( tcConfig.getValueMode() ) {
				case IDENTITY: {
					throw new CacheException(
							"The clustered Hibernate cache " + cache.getName() + " is using IDENTITY value mode.\n"
									+ "Identity value mode cannot be used with Hibernate cache regions."
					);
				}
				case SERIALIZATION:
				default: {
					// this is the recommended valueMode
					break;
				}
			}
		}
	}
}
